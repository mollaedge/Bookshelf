package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.dto.DtoSignalRequest;
import com.arturmolla.bookshelf.model.dto.DtoStreamEvent;
import com.arturmolla.bookshelf.model.dto.DtoStreamInfo;
import com.arturmolla.bookshelf.model.dto.DtoStreamStartRequest;
import com.arturmolla.bookshelf.model.enums.StreamEventType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.service.stream.LiveStream;
import com.arturmolla.bookshelf.service.stream.StreamRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceStream {

    /**
     * SSE timeout: 6 hours. The client must reconnect if the stream runs longer.
     */
    private static final long SSE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(6);

    private final StreamRegistry registry;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // START
    // =========================================================================

    /**
     * Starts a new stream for the authenticated user.
     * Only one stream per user is allowed at any time.
     *
     * @return an {@link SseEmitter} that keeps the host's connection alive and
     * delivers stream events to them in real time
     */
    public SseEmitter startStream(DtoStreamStartRequest request, Authentication auth) {
        User host = principal(auth);

        if (registry.hasStream(host.getId())) {
            throw new OperationNotPermittedException(
                    "You already have an active stream. Stop it before starting a new one.");
        }

        LiveStream stream = new LiveStream(host.getId(), host.getFullName(), request.title());
        registry.register(stream);
        log.info("Stream started: hostId={} title='{}'", host.getId(), request.title());

        SseEmitter emitter = createEmitter(stream, host.getId());
        stream.addParticipant(host.getId(), host.getFullName(), emitter);

        // Notify the host that the stream is live
        sendToOne(emitter, buildEvent(StreamEventType.STREAM_STARTED, stream, host));
        return emitter;
    }

    // =========================================================================
    // JOIN
    // =========================================================================

    /**
     * Joins an existing stream as a watcher.
     *
     * @param hostId the userId of the stream host (stream identifier)
     * @return an {@link SseEmitter} that pushes events to this watcher
     */
    public SseEmitter joinStream(Long hostId, Authentication auth) {
        User watcher = principal(auth);
        LiveStream stream = findOrThrow(hostId);

        if (watcher.getId().equals(hostId)) {
            // Host is reconnecting (e.g. page refresh) — replace their emitter
            SseEmitter old = stream.getEmitter(hostId);
            if (old != null) old.complete();
        } else if (stream.hasParticipant(watcher.getId())) {
            // Watcher is already connected — return a fresh emitter replacing the old one
            SseEmitter old = stream.getEmitter(watcher.getId());
            if (old != null) old.complete();
        }

        SseEmitter emitter = createEmitter(stream, watcher.getId());
        stream.addParticipant(watcher.getId(), watcher.getFullName(), emitter);
        log.info("Watcher joined: userId={} streamId={}", watcher.getId(), hostId);

        // Tell this watcher about the current stream state
        sendToOne(emitter, buildEvent(StreamEventType.WATCHER_JOINED, stream, watcher));

        // Notify every OTHER participant that someone joined
        broadcastExcept(stream, watcher.getId(),
                buildEvent(StreamEventType.WATCHER_JOINED, stream, watcher));

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

        broadcastAll(stream, buildEvent(StreamEventType.WATCHER_LEFT, stream, watcher));
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
        broadcastAll(stream, buildEvent(StreamEventType.STREAM_STOPPED, stream, host));
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
     * <p>The {@code payload} inside {@link DtoSignalRequest} is forwarded verbatim
     * so the front-end can parse it as JSON without any server-side knowledge of
     * the WebRTC handshake format.</p>
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
                .actorId(sender.getId())
                .actorName(sender.getFullName())
                .payload(request.type() + ":" + request.payload()) // prefix type for FE parsing
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

    /**
     * Returns metadata for all currently active streams.
     */
    public List<DtoStreamInfo> listStreams() {
        return registry.allStreams().stream()
                .map(this::toInfo)
                .toList();
    }

    /**
     * Returns metadata for a single stream.
     */
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

        Runnable cleanup = () -> {
            if (!stream.hasParticipant(userId)) return; // already cleaned up

            // Capture name BEFORE removing the participant
            String displayName = stream.getParticipantNames().getOrDefault(userId, "unknown");
            stream.removeParticipant(userId);
            log.info("SSE emitter cleaned up: userId={} streamId={}", userId, stream.getHostId());

            if (userId.equals(stream.getHostId())) {
                // Host disconnected — terminate the stream for everyone
                log.info("Host disconnected — stopping stream hostId={}", stream.getHostId());
                DtoStreamEvent stopEvent = DtoStreamEvent.builder()
                        .type(StreamEventType.STREAM_STOPPED)
                        .streamId(stream.getHostId())
                        .actorId(userId)
                        .actorName(displayName)
                        .watcherCount(0)
                        .build();
                broadcastAll(stream, stopEvent);
                stream.closeAll();
                registry.remove(stream.getHostId());
            } else {
                // Watcher dropped — notify everyone still connected
                DtoStreamEvent leftEvent = DtoStreamEvent.builder()
                        .type(StreamEventType.WATCHER_LEFT)
                        .streamId(stream.getHostId())
                        .actorId(userId)
                        .actorName(displayName)
                        .watcherCount(stream.getWatcherCount())
                        .build();
                broadcastAll(stream, leftEvent);
            }
        };

        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        // onCompletion fires for normal completions too (e.g. stopStream calling closeAll),
        // so only run cleanup if the participant is still registered.
        emitter.onCompletion(cleanup);

        return emitter;
    }

    private DtoStreamEvent buildEvent(StreamEventType type, LiveStream stream, User actor) {
        return DtoStreamEvent.builder()
                .type(type)
                .streamId(stream.getHostId())
                .actorId(actor.getId())
                .actorName(actor.getFullName())
                .watcherCount(stream.getWatcherCount())
                .build();
    }

    /**
     * Sends an event to a single emitter; removes the participant on failure.
     */
    @Async
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

    /**
     * Broadcasts an event to every participant in the stream.
     */
    private void broadcastAll(LiveStream stream, DtoStreamEvent event) {
        stream.getEmitters().forEach((uid, emitter) -> sendToOne(emitter, event));
    }

    /**
     * Broadcasts an event to every participant EXCEPT the excluded userId.
     */
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

