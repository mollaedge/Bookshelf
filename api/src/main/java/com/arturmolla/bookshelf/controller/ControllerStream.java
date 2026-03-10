package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.dto.DtoSignalRequest;
import com.arturmolla.bookshelf.model.dto.DtoStreamInfo;
import com.arturmolla.bookshelf.service.ServiceStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Live streaming controller.
 *
 * <h2>Flow</h2>
 * <pre>
 * Host                              Server                          Watcher
 *  │                                   │                               │
 *  │── POST /streams/start ───────────▶│  create LiveStream            │
 *  │◀─ SSE connection (host emitter) ──│                               │
 *  │                                   │                               │
 *  │                                   │◀── GET /streams/{id}/join ────│
 *  │                                   │    add watcher emitter        │
 *  │◀─ WATCHER_JOINED event ───────────│──▶ WATCHER_JOINED event ─────│
 *  │                                   │                               │
 *  │── POST /streams/{id}/signal ─────▶│  relay SDP offer              │
 *  │                                   │──▶ SIGNAL event ─────────────│
 *  │◀─ SIGNAL event (SDP answer) ──────│◀── POST /streams/{id}/signal ─│
 *  │                                   │                               │
 *  │   (WebRTC peer-to-peer video) ────┼───────────────────────────────│
 *  │                                   │                               │
 *  │── DELETE /streams/stop ──────────▶│  broadcast STREAM_STOPPED     │
 *  │                                   │──▶ STREAM_STOPPED ───────────│
 * </pre>
 *
 * <h2>SSE event names</h2>
 * <ul>
 *   <li>{@code STREAM_STARTED}  – host's own emitter receives this on start</li>
 *   <li>{@code WATCHER_JOINED}  – broadcast when someone joins</li>
 *   <li>{@code WATCHER_LEFT}    – broadcast when someone leaves / disconnects</li>
 *   <li>{@code STREAM_STOPPED}  – broadcast when host stops the stream</li>
 *   <li>{@code SIGNAL}          – WebRTC SDP / ICE relay; payload = "type:jsonPayload"</li>
 * </ul>
 *
 * <h2>Front-end usage</h2>
 * <pre>
 * // Host
 * const es = new EventSource('/streams/start', { headers: { Authorization: 'Bearer ...' }});
 * es.addEventListener('STREAM_STARTED', e => console.log(JSON.parse(e.data)));
 * es.addEventListener('WATCHER_JOINED', e => initiateWebRTC(JSON.parse(e.data).actorId));
 * es.addEventListener('SIGNAL',         e => handleSignal(JSON.parse(e.data)));
 *
 * // Watcher
 * const es = new EventSource('/streams/{hostId}/join', { headers: { Authorization: 'Bearer ...' }});
 * es.addEventListener('WATCHER_JOINED', e => console.log('I joined', JSON.parse(e.data)));
 * es.addEventListener('STREAM_STOPPED', e => { es.close(); showStreamEnded(); });
 * es.addEventListener('SIGNAL',         e => handleSignal(JSON.parse(e.data)));
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("streams")
@Tag(name = "Live Streaming")
@Validated
public class ControllerStream {

    private final ServiceStream serviceStream;

    // =========================================================================
    // HOST — start / stop
    // =========================================================================

    /**
     * Start a new live stream for the authenticated user.
     * <p>
     * Uses GET so the browser's native {@code EventSource} API can connect directly:
     * <pre>
     * const es = new EventSource('/streams/start?title=My+Stream');
     * </pre>
     * Returns a long-lived SSE connection. Only ONE stream per user is allowed.
     *
     * @param title the stream title (max 120 chars, required)
     */
    @GetMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Start a new live stream (one per user) — SSE")
    public SseEmitter startStream(
            @RequestParam
            @NotBlank(message = "Stream title must not be blank")
            @Size(max = 120, message = "Title must be at most 120 characters")
            String title,
            Authentication connectedUser
    ) {
        return serviceStream.startStream(title, connectedUser);
    }

    /**
     * Stop the authenticated user's stream.
     * Broadcasts a {@code STREAM_STOPPED} event to all watchers and closes
     * every SSE connection.
     */
    @DeleteMapping("/stop")
    @Operation(summary = "Stop the authenticated user's stream")
    public ResponseEntity<Void> stopStream(Authentication connectedUser) {
        serviceStream.stopStream(connectedUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // WATCHER — join / leave
    // =========================================================================

    /**
     * Join an active stream as a watcher.
     * <p>
     * Returns a long-lived SSE connection. The watcher receives:
     * <ul>
     *   <li>{@code WATCHER_JOINED} – immediately on connect (confirms join)</li>
     *   <li>{@code WATCHER_LEFT}   – when another watcher leaves</li>
     *   <li>{@code STREAM_STOPPED} – when the host ends the stream</li>
     *   <li>{@code SIGNAL}         – WebRTC SDP / ICE targeted at this watcher</li>
     * </ul>
     *
     * @param hostId the userId of the stream's host (the stream identifier)
     */
    @GetMapping(value = "/{host-id}/join", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Join a live stream as a watcher")
    public SseEmitter joinStream(
            @PathVariable("host-id") Long hostId,
            Authentication connectedUser
    ) {
        return serviceStream.joinStream(hostId, connectedUser);
    }

    /**
     * Leave a stream (watcher only).
     * Broadcasts a {@code WATCHER_LEFT} event to remaining participants.
     * Hosts must use {@code DELETE /streams/stop} instead.
     */
    @DeleteMapping("/{host-id}/leave")
    @Operation(summary = "Leave a stream (watcher only)")
    public ResponseEntity<Void> leaveStream(
            @PathVariable("host-id") Long hostId,
            Authentication connectedUser
    ) {
        serviceStream.leaveStream(hostId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // SIGNALLING (WebRTC SDP / ICE)
    // =========================================================================

    /**
     * Send a WebRTC signalling message (SDP offer/answer or ICE candidate).
     * <p>
     * The server relays the message via SSE to the target participant (or
     * broadcasts to all if {@code targetUserId} is null).
     * <p>
     * <strong>Signal payload format</strong> – the FE receives an SSE event
     * with name {@code SIGNAL} and data JSON:
     * <pre>
     * {
     *   "type":       "SIGNAL",
     *   "streamId":   123,
     *   "actorId":    456,
     *   "actorName":  "Jane Doe",
     *   "payload":    "offer:{...sdp json...}",
     *   "watcherCount": 3
     * }
     * </pre>
     * The {@code payload} field is prefixed with the signal type and a colon
     * so the FE can split on the first {@code :} to determine the action.
     *
     * @param hostId the stream identifier (host's userId)
     */
    @PostMapping("/{host-id}/signal")
    @Operation(summary = "Relay a WebRTC signal to another participant")
    public ResponseEntity<Void> signal(
            @PathVariable("host-id") Long hostId,
            @Valid @RequestBody DtoSignalRequest request,
            Authentication connectedUser
    ) {
        serviceStream.signal(hostId, request, connectedUser);
        return ResponseEntity.accepted().build();
    }

    // =========================================================================
    // DISCOVERY
    // =========================================================================

    /**
     * List all currently active streams.
     */
    @GetMapping
    @Operation(summary = "List all active streams")
    public ResponseEntity<List<DtoStreamInfo>> listStreams() {
        return ResponseEntity.ok(serviceStream.listStreams());
    }

    /**
     * Get info/metadata for a specific active stream.
     *
     * @param hostId the stream identifier (host's userId)
     */
    @GetMapping("/{host-id}/info")
    @Operation(summary = "Get metadata of a specific stream")
    public ResponseEntity<DtoStreamInfo> getStreamInfo(
            @PathVariable("host-id") Long hostId
    ) {
        return ResponseEntity.ok(serviceStream.getStreamInfo(hostId));
    }
}

