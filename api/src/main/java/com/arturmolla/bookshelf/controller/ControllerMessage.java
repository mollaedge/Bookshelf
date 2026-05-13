package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.aspects.annotation.RateLimit;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoConversationResponse;
import com.arturmolla.bookshelf.model.dto.DtoMessageRequest;
import com.arturmolla.bookshelf.model.dto.DtoMessageResponse;
import com.arturmolla.bookshelf.service.ServiceMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Direct-messaging controller for friends.
 *
 * <h2>Real-time flow</h2>
 * <pre>
 * Client A (sender)                    Server                       Client B (recipient)
 *   │                                     │                                │
 *   │── GET /messages/connect ───────────▶│  register SseEmitter[A]        │
 *   │                            [already connected]                       │
 *   │                                     │◀── GET /messages/connect ──────│
 *   │                                     │    register SseEmitter[B]      │
 *   │                                     │                                │
 *   │── POST /messages/{friendBId} ──────▶│  persist EntityMessage         │
 *   │◀─ 200 DtoMessageResponse ───────────│                                │
 *   │                                     │──▶ SSE "NEW_MESSAGE" ─────────▶│
 *   │                                     │                                │
 *   │                                     │◀── PATCH /messages/{id}/read ──│
 *   │◀─ SSE "MESSAGE_READ" ───────────────│                                │
 * </pre>
 *
 * <h2>SSE event names</h2>
 * <ul>
 *   <li>{@code NEW_MESSAGE}  – pushed to the recipient when a friend sends a message</li>
 *   <li>{@code MESSAGE_READ} – pushed to the sender when the recipient marks a message as read</li>
 *   <li>{@code heartbeat}    – keep-alive comment; the front end can safely ignore it</li>
 * </ul>
 *
 * <h2>Front-end usage</h2>
 * <pre>
 * // Open the SSE channel once (e.g. in App.jsx on login)
 * const es = new EventSource('/messages/connect', { withCredentials: true });
 * es.addEventListener('NEW_MESSAGE',  e => appendMessage(JSON.parse(e.data)));
 * es.addEventListener('MESSAGE_READ', e => updateReadStatus(JSON.parse(e.data)));
 * es.addEventListener('heartbeat',    () => {});   // keep-alive – ignore
 *
 * // Send a message
 * await axios.post(`/messages/${friendId}`, { content: 'Hey!' });
 *
 * // Fetch history
 * const { data } = await axios.get(`/messages/conversations/${friendId}?page=0&amp;size=50`);
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("messages")
@Tag(name = "Messages", description = "Real-time direct messaging between friends")
public class ControllerMessage {

    private final ServiceMessage serviceMessage;

    // =========================================================================
    // SSE – subscribe to real-time events
    // =========================================================================

    /**
     * Opens a long-lived SSE channel for the authenticated user.
     * <p>
     * Keep this connection open to receive incoming messages instantly.
     * The server sends a {@code heartbeat} comment every 25 s to prevent
     * Cloudflare / Nginx from killing the idle connection.
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time message events (SSE)")
    public SseEmitter connect(Authentication connectedUser, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return serviceMessage.connect(connectedUser);
    }

    // =========================================================================
    // Send a message
    // =========================================================================

    /**
     * Sends a direct message to a friend.
     * <p>
     * Fails with {@code 403} if the two users are not accepted friends.
     * On success the message is also pushed to the recipient via SSE
     * ({@code NEW_MESSAGE} event) if they are currently connected.
     *
     * @param friendId ID of the recipient
     */
    @PostMapping(path = "/{friendId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send a message to a friend (with optional media)")
    @RateLimit(capacity = 60, refillTokens = 60, refillDurationMinutes = 1)
    public ResponseEntity<DtoMessageResponse> sendMessage(
            @Parameter(description = "ID of the friend to message")
            @PathVariable Long friendId,
            @ModelAttribute DtoMessageRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceMessage.sendMessage(friendId, request, connectedUser));
    }

    // =========================================================================
    // Conversation list (inbox)
    // =========================================================================

    /**
     * Returns all conversations for the authenticated user, most-recently-active first.
     * Each item includes the friend's name, a preview of the last message, and
     * the count of unread messages in that conversation.
     */
    @GetMapping("/conversations")
    @Operation(summary = "List all conversations (inbox)")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoConversationResponse>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceMessage.getConversations(page, size, connectedUser));
    }

    /**
     * Returns the number of conversations with unread messages.
     */
    @GetMapping("/conversations/unread-count")
    @Operation(summary = "Count conversations with unread messages")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<Long> getUnreadConversationCount(Authentication connectedUser) {
        return ResponseEntity.ok(serviceMessage.getUnreadConversationCount(connectedUser));
    }

    // =========================================================================
    // Message history
    // =========================================================================

    /**
     * Returns the message history with a specific friend (newest first).
     * <p>
     * Automatically marks all unread messages from the friend as read.
     *
     * @param friendId ID of the other participant
     */
    @GetMapping("/conversations/{friendId}")
    @Operation(summary = "Get message history with a friend (latest first)")
    @RateLimit(capacity = 30, refillTokens = 30, refillDurationMinutes = 1)
    public ResponseEntity<PageResponse<DtoMessageResponse>> getMessages(
            @Parameter(description = "ID of the friend whose conversation to open")
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceMessage.getMessages(friendId, page, size, connectedUser));
    }

    // =========================================================================
    // Mark a single message as read
    // =========================================================================

    /**
     * Marks a specific message as read.
     * <p>
     * Only the recipient (not the sender) may call this endpoint.
     * On success, a {@code MESSAGE_READ} SSE event is pushed to the original sender
     * if they are currently connected.
     *
     * @param messageId ID of the message to mark as read
     */
    @PatchMapping("/{messageId}/read")
    @Operation(summary = "Mark a message as read")
    @RateLimit(capacity = 60, refillTokens = 60, refillDurationMinutes = 1)
    public ResponseEntity<DtoMessageResponse> markAsRead(
            @Parameter(description = "ID of the message to mark as read")
            @PathVariable Long messageId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceMessage.markAsRead(messageId, connectedUser));
    }

    // =========================================================================
    // Delete a message
    // =========================================================================

    /**
     * Deletes a message.
     * <p>
     * Only the original sender may delete their own message.
     *
     * @param messageId ID of the message to delete
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete a message (sender only)")
    @RateLimit(capacity = 20, refillTokens = 20, refillDurationMinutes = 1)
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "ID of the message to delete")
            @PathVariable Long messageId,
            Authentication connectedUser
    ) {
        serviceMessage.deleteMessage(messageId, connectedUser);
        return ResponseEntity.noContent().build();
    }
}