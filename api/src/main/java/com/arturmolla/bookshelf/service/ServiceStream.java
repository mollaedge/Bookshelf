package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.WebRtcProperties;
import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.dto.DtoIceServer;
import com.arturmolla.bookshelf.model.dto.DtoSignalRequest;
import com.arturmolla.bookshelf.model.dto.DtoStreamEvent;
import com.arturmolla.bookshelf.model.dto.DtoStreamInfo;
import com.arturmolla.bookshelf.model.enums.StreamEventType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.service.stream.LiveStream;
import com.arturmolla.bookshelf.service.stream.StreamRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceStream {

    /**
     * SSE timeout: 6 hours. The client must reconnect if the stream runs longer.
     */
    private static final long SSE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(6);

    /**
     * Heartbeat interval: 30 seconds. Keeps the SSE connection alive through
     * Cloudflare (100s timeout) and Nginx proxies that buffer idle connections.
     */
    private static final long HEARTBEAT_INTERVAL_SEC = 30;

    private static final ScheduledExecutorService heartbeatExecutor =
            Executors.newScheduledThreadPool(4);

    private final StreamRegistry registry;
    private final ObjectMapper objectMapper;
    private final WebRtcProperties webRtcProperties;

    // =========================================================================
    // ICE SERVERS
    // =========================================================================

    /**
     * Returns the configured ICE (STUN/TURN) servers.
     * The front-end should call this before creating any {@code RTCPeerConnection}.
     */
    public List<DtoIceServer> getIceServers() {
        return webRtcProperties.getIceServers().stream()
                .map(s -> DtoIceServer.builder()
                        .urls(s.getUrls())
                        .username(s.getUsername())
                        .credential(s.getCredential())
                        .build())
                .toList();
    }

    // =========================================================================
    // START
    // =========================================================================

    /**
     * Starts a new stream for the authenticated user.
     * Only one stream per user is allowed at any time.
     */
    public SseEmitter startStream(String title, Authentication auth) {
        User host = principal(auth);

        if (registry.hasStream(host.getId())) {
            throw new OperationNotPermittedException(
                    "You already have an active stream. Stop it before starting a new one.");
        }

        LiveStream stream = new LiveStream(host.getId(), host.getFullName(), title);
        registry.register(stream);
        log.info("Stream started: hostId={} title='{}'", host.getId(), title);

        SseEmitter emitter = createEmitter(stream, host.getId());
        stream.addParticipant(host.getId(), host.getFullName(), emitter);

        // Notify the host that the stream is live
        sendToOne(emitter, buildEvent(StreamEventType.STREAM_STARTED, stream, host, null));
        return emitter;
    }

    // =========================================================================
    // JOIN
    // =========================================================================

    /**
     * Joins an existing stream as a watcher.
     *
     * @param hostId the userId of the stream host (stream identifier)
     */
    public SseEmitter joinStream(Long hostId, Authentication auth) {
        User watcher = principal(auth);
        LiveStream stream = findOrThrow(hostId);

        if (watcher.getId().equals(hostId)) {
            // Host is reconnecting (e.g. page refresh) — replace their emitter silently.
            // silentRemove removes the host from the maps first so that when we call
            // old.complete() the cleanup callback sees hasParticipant==false and returns
            // early — preventing a spurious STREAM_STOPPED broadcast + registry.remove().
            SseEmitter old = stream.silentRemove(hostId);
            if (old != null) old.complete();

            SseEmitter emitter = createEmitter(stream, hostId);
            stream.addParticipant(hostId, watcher.getFullName(), emitter);
            log.info("Host SSE reconnected: hostId={}", hostId);

            // Re-send STREAM_STARTED so the host UI restores its state
            sendToOne(emitter, buildEvent(StreamEventType.STREAM_STARTED, stream, watcher, null));
            return emitter;
        }

        if (stream.hasParticipant(watcher.getId())) {
            // Watcher is reconnecting — silently swap the old emitter so the cleanup
            // callback is a no-op and does NOT broadcast WATCHER_LEFT / WATCHER_JOINED.
            SseEmitter old = stream.silentRemove(watcher.getId());
            if (old != null) old.complete();
        }

        SseEmitter emitter = createEmitter(stream, watcher.getId());
        stream.addParticipant(watcher.getId(), watcher.getFullName(), emitter);
        log.info("Watcher joined: userId={} streamId={}", watcher.getId(), hostId);

        // Tell THIS watcher about the current stream state (confirms they have joined)
        sendToOne(emitter, buildEvent(StreamEventType.WATCHER_JOINED, stream, watcher, null));

        // Notify every OTHER participant (especially the HOST) that someone joined.
        // The HOST uses actorId from this event to know which watcher to send an SDP offer to.
        broadcastExcept(stream, watcher.getId(),
                buildEvent(StreamEventType.WATCHER_JOINED, stream, watcher, null));

        return emitter;
    }

    // =========================================================================
    // LEAVE
    // =========================================================================

    /**
     * Removes the authenticated user from the watcher list.
     * If called by the host, use {@link #stopStream} instead.
     */
    public void leaveStream(Long hostId, Authentication auth) {
        User watcher = principal(auth);
        LiveStream stream = findOrThrow(hostId);

        if (watcher.getId().equals(hostId)) {
            throw new OperationNotPermittedException(
                    "As the host, use the stop endpoint to end your stream.");
        }

        stream.removeParticipant(watcher.getId());
        log.info("Watcher left: userId={} streamId={}", watcher.getId(), hostId);

        broadcastAll(stream, buildEvent(StreamEventType.WATCHER_LEFT, stream, watcher, null));
    }

    // =========================================================================
    // STOP
    // =========================================================================

    /**
     * Stops the stream. Only the host can call this.
     * All connected SSE emitters are completed (FE {@code EventSource} closes).
     */
    public void stopStream(Authentication auth) {
        User host = principal(auth);
        LiveStream stream = findOrThrow(host.getId());

        if (!Objects.equals(stream.getHostId(), host.getId())) {
            throw new OperationNotPermittedException("Only the host can stop this stream.");
        }

        // Broadcast the stop event before closing emitters so FE receives it
        broadcastAll(stream, buildEvent(StreamEventType.STREAM_STOPPED, stream, host, null));
        stream.closeAll();
        registry.remove(host.getId());
        log.info("Stream stopped: hostId={}", host.getId());
    }

    // =========================================================================
    // SIGNAL (WebRTC SDP / ICE relay)
    // =========================================================================

    /**
     * Relays a WebRTC signalling message (SDP offer/answer or ICE candidate)
     * from the sender to a specific target, or broadcasts to all if targetUserId is null.
     *
     * <p><strong>Ordering guarantee:</strong> sendToOne is synchronous —
     * signals are delivered in the exact order they arrive at the server,
     * which is critical for the WebRTC handshake (offer → answer → ICE candidates).</p>
     *
     * <p>Signal types the front-end should handle:</p>
     * <ul>
     *   <li>{@code offer}         – host sent an SDP offer; watcher must answer</li>
     *   <li>{@code answer}        – watcher replied; host sets remote description</li>
     *   <li>{@code ice-candidate} – ICE candidate from either peer; add to RTCPeerConnection</li>
     * </ul>
     */
    public void signal(Long hostId, DtoSignalRequest request, Authentication auth) {
        User sender = principal(auth);
        LiveStream stream = findOrThrow(hostId);

        if (!stream.hasParticipant(sender.getId())) {
            throw new OperationNotPermittedException("You are not a participant of this stream.");
        }

        DtoStreamEvent event = DtoStreamEvent.builder()
                .type(StreamEventType.SIGNAL)
                .streamId(hostId)
                .streamTitle(stream.getTitle())
                .actorId(sender.getId())
                .actorName(sender.getFullName())
                .targetUserId(request.targetUserId())
                .signalType(request.signalType())
                .payload(request.payload())
                .watcherCount(stream.getWatcherCount())
                .build();

        if (request.targetUserId() != null) {
            SseEmitter target = stream.getEmitter(request.targetUserId());
            if (target == null) {
                throw new EntityNotFoundException(
                        "Target user " + request.targetUserId() + " is not connected to this stream.");
            }
            sendToOne(target, event);
        } else {
            broadcastExcept(stream, sender.getId(), event);
        }
    }

    // =========================================================================
    // LIST
    // =========================================================================

    /** Returns metadata for all currently active streams. */
    public List<DtoStreamInfo> listStreams() {
        return registry.allStreams().stream()
                .map(this::toInfo)
                .toList();
    }

    /** Returns metadata for a single stream. */
    public DtoStreamInfo getStreamInfo(Long hostId) {
        return toInfo(findOrThrow(hostId));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private LiveStream findOrThrow(Long hostId) {
        return registry.find(hostId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active stream found for hostId: " + hostId));
    }

    private User principal(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    /**
     * Creates an {@link SseEmitter} and wires up cleanup callbacks so the participant
     * is automatically removed from the stream on timeout or connection error.
     */
    private SseEmitter createEmitter(LiveStream stream, Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cleaned = new AtomicBoolean(false);

        // Heartbeat: send a comment every 30 s to prevent Cloudflare/Nginx from
        // closing the idle SSE connection with 502/504.
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException e) {
                // The error/completion callbacks will handle cleanup
            }
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);

        Runnable cleanup = () -> {
            if (!cleaned.compareAndSet(false, true)) return;
            heartbeat.cancel(false);
            if (!stream.hasParticipant(userId)) return;

            // Use silentRemove so we only remove from the maps without calling
            // emitter.complete() again (the emitter is already completing/timed-out).
            String displayName = stream.getParticipantNames().getOrDefault(userId, "unknown");
            stream.silentRemove(userId);
            log.info("SSE emitter cleaned up: userId={} streamId={}", userId, stream.getHostId());

            if (userId.equals(stream.getHostId())) {
                log.info("Host disconnected — stopping stream hostId={}", stream.getHostId());
                DtoStreamEvent stopEvent = DtoStreamEvent.builder()
                        .type(StreamEventType.STREAM_STOPPED)
                        .streamId(stream.getHostId())
                        .streamTitle(stream.getTitle())
                        .actorId(userId)
                        .actorName(displayName)
                        .watcherCount(0)
                        .build();
                broadcastAll(stream, stopEvent);
                stream.closeAll();
                registry.remove(stream.getHostId());
            } else {
                DtoStreamEvent leftEvent = DtoStreamEvent.builder()
                        .type(StreamEventType.WATCHER_LEFT)
                        .streamId(stream.getHostId())
                        .streamTitle(stream.getTitle())
                        .actorId(userId)
                        .actorName(displayName)
                        .watcherCount(stream.getWatcherCount())
                        .build();
                broadcastAll(stream, leftEvent);
            }
        };

        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        emitter.onCompletion(cleanup);

        return emitter;
    }

    private DtoStreamEvent buildEvent(StreamEventType type, LiveStream stream, User actor, Long targetUserId) {
        return DtoStreamEvent.builder()
                .type(type)
                .streamId(stream.getHostId())
                .streamTitle(stream.getTitle())
                .actorId(actor.getId())
                .actorName(actor.getFullName())
                .targetUserId(targetUserId)
                .watcherCount(stream.getWatcherCount())
                .build();
    }

    /**
     * Sends an event synchronously to a single emitter.
     *
     * <p><strong>MUST remain synchronous</strong> — WebRTC signalling requires
     * strict ordering (offer → answer → ICE candidates). Making this async
     * causes race conditions that result in failed peer connections and a
     * blank black video screen on the watcher side.</p>
     */
    public void sendToOne(SseEmitter emitter, DtoStreamEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.type().name())
                    .data(toJson(event)));
        } catch (IOException e) {
            log.warn("Failed to send SSE event to emitter: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    /** Broadcasts an event to every participant in the stream. */
    private void broadcastAll(LiveStream stream, DtoStreamEvent event) {
        stream.getEmitters().forEach((uid, emitter) -> sendToOne(emitter, event));
    }

    /** Broadcasts an event to every participant EXCEPT the excluded userId. */
    private void broadcastExcept(LiveStream stream, Long excludeUserId, DtoStreamEvent event) {
        stream.getEmitters().forEach((uid, emitter) -> {
            if (!uid.equals(excludeUserId)) sendToOne(emitter, event);
        });
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error", e);
            return "{}";
        }
    }

    private DtoStreamInfo toInfo(LiveStream stream) {
        return DtoStreamInfo.builder()
                .streamId(stream.getHostId())
                .hostId(stream.getHostId())
                .hostName(stream.getHostName())
                .title(stream.getTitle())
                .startedAt(stream.getStartedAt())
                .watcherCount(stream.getWatcherCount())
                .watcherNames(stream.getWatcherNames())
                .build();
    }
}

