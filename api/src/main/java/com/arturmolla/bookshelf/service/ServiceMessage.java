package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.config.exceptions.OperationNotPermittedException;
import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoConversationResponse;
import com.arturmolla.bookshelf.model.dto.DtoMessageReplySnippet;
import com.arturmolla.bookshelf.model.dto.DtoMessageRequest;
import com.arturmolla.bookshelf.model.dto.DtoMessageResponse;
import com.arturmolla.bookshelf.model.entity.EntityConversation;
import com.arturmolla.bookshelf.model.entity.EntityMessage;
import com.arturmolla.bookshelf.model.enums.RelationStatus;
import com.arturmolla.bookshelf.model.enums.RelationType;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryConversation;
import com.arturmolla.bookshelf.repository.RepositoryMessage;
import com.arturmolla.bookshelf.repository.RepositoryUser;
import com.arturmolla.bookshelf.repository.RepositoryUserRelation;
import com.arturmolla.bookshelf.service.messaging.MessageEmitterRegistry;
import com.arturmolla.bookshelf.service.ServiceFileStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
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
@Transactional
public class ServiceMessage {

    // ─── SSE configuration ────────────────────────────────────────────────────
    /** Keep SSE connections open for up to 1 hour before the client must reconnect. */
    private static final long SSE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);
    private static final long HEARTBEAT_INTERVAL_SEC = 25;

    private static final ScheduledExecutorService heartbeatExecutor =
            Executors.newScheduledThreadPool(2);

    // ─── SSE event name constants ─────────────────────────────────────────────
    public static final String EVENT_NEW_MESSAGE  = "NEW_MESSAGE";
    public static final String EVENT_MESSAGE_READ = "MESSAGE_READ";

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final RepositoryConversation repositoryConversation;
    private final RepositoryMessage      repositoryMessage;
    private final RepositoryUser         repositoryUser;
    private final RepositoryUserRelation repositoryUserRelation;
    private final MessageEmitterRegistry emitterRegistry;
    private final ObjectMapper           objectMapper;
    private final ServiceFileStorage     serviceFileStorage;

    // =========================================================================
    // SSE – connect / disconnect
    // =========================================================================

    /**
     * Opens a long-lived SSE channel for the authenticated user.
     * <p>
     * The client must keep this connection open to receive incoming messages
     * in real time.  A heartbeat comment is sent every 25 s to prevent
     * Cloudflare / Nginx from closing the idle connection.
     *
     * <h3>Front-end example</h3>
     * <pre>
     * const es = new EventSource('/messages/connect', { withCredentials: true });
     * es.addEventListener('NEW_MESSAGE',  e => appendMessage(JSON.parse(e.data)));
     * es.addEventListener('MESSAGE_READ', e => markDelivered(JSON.parse(e.data)));
     * es.addEventListener('heartbeat',    () => {});  // keep-alive — ignore
     * </pre>
     */
    public SseEmitter connect(Authentication auth) {
        User user = principal(auth);

        // If the user reconnects, cleanly close the old emitter first.
        emitterRegistry.find(user.getId()).ifPresent(old -> {
            emitterRegistry.remove(user.getId());
            old.complete();
        });

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cleaned = new AtomicBoolean(false);

        // Heartbeat to keep the connection alive through reverse-proxies.
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException ignored) {
                // The error callback will handle cleanup.
            }
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);

        Runnable cleanup = () -> {
            if (!cleaned.compareAndSet(false, true)) return;
            heartbeat.cancel(false);
            emitterRegistry.remove(user.getId());
            log.info("Messaging SSE disconnected: userId={}", user.getId());
        };

        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        emitter.onCompletion(cleanup);

        emitterRegistry.register(user.getId(), emitter);
        log.info("Messaging SSE connected: userId={}", user.getId());
        return emitter;
    }

    // =========================================================================
    // Send a message
    // =========================================================================

    /**
     * Sends a message from the authenticated user to {@code friendId}.
     * <p>
     * Only accepted friends may message each other.  The conversation row is
     * created on-the-fly the first time two friends exchange a message.
     * <p>
     * If the recipient currently has an open SSE connection, the message is
     * pushed to them instantly via a {@code NEW_MESSAGE} event.
     *
     * @param friendId ID of the recipient
     * @param request  message body
     * @param auth     Spring Security authentication of the sender
     * @return the persisted message as a DTO
     * @throws OperationNotPermittedException if the two users are not friends
     */
    public DtoMessageResponse sendMessage(Long friendId, DtoMessageRequest request, Authentication auth) {
        User sender = principal(auth);
        User recipient = findUserOrThrow(friendId);
        ensureFriendship(sender.getId(), recipient.getId());
        EntityConversation conversation = repositoryConversation
                .findByUsers(sender.getId(), recipient.getId())
                .orElseGet(() -> createConversation(sender, recipient));
        EntityMessage replyTo = null;
        if (request.getReplyToId() != null) {
            replyTo = repositoryMessage.findById(request.getReplyToId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Message not found: " + request.getReplyToId()));
            if (!replyTo.getConversation().getId().equals(conversation.getId())) {
                throw new OperationNotPermittedException(
                        "You can only reply to messages within the same conversation.");
            }
        }
        // Handle media
        byte[] mediaData = null;
        String mediaType = null;
        String mediaName = null;
        Long mediaSize = null;
        MultipartFile media = request.getMedia();
        if (media != null && !media.isEmpty()) {
            try {
                mediaData = media.getBytes();
                mediaType = media.getContentType();
                mediaName = media.getOriginalFilename();
                mediaSize = (long) mediaData.length;
                // If image and too large, compress
                if (mediaType != null && mediaType.startsWith("image") && mediaData.length > ServiceFileStorage.COMPRESS_THRESHOLD_BYTES) {
                    mediaData = serviceFileStorage.compressImageBytes(mediaData);
                    mediaType = "image/jpeg";
                    mediaSize = (long) mediaData.length;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not process uploaded media", e);
            }
        }
        EntityMessage message = EntityMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .replyTo(replyTo)
                .mediaData(mediaData)
                .mediaType(mediaType)
                .mediaName(mediaName)
                .mediaSize(mediaSize)
                .build();
        message = repositoryMessage.save(message);
        conversation.setLastMessageAt(Instant.now());
        repositoryConversation.save(conversation);
        DtoMessageResponse response = toMessageDto(message);
        pushToUser(recipient.getId(), EVENT_NEW_MESSAGE, response);
        log.debug("Message sent: senderId={} recipientId={} conversationId={}",
                sender.getId(), recipient.getId(), conversation.getId());
        return response;
    }

    // =========================================================================
    // Conversation list
    // =========================================================================

    /**
     * Returns all conversations for the authenticated user, most-recent first.
     * Each item includes an unread-count and a preview of the last message.
     */
    public PageResponse<DtoConversationResponse> getConversations(int page, int size, Authentication auth) {
        User user = principal(auth);
        Page<EntityConversation> result = repositoryConversation
                .findByUserId(user.getId(), PageRequest.of(page, size));

        List<DtoConversationResponse> content = result.getContent()
                .stream()
                .map(c -> toConversationDto(c, user))
                .toList();

        return PageResponse.<DtoConversationResponse>builder()
                .content(content)
                .number(result.getNumber())
                .size(result.getSize())
                .totalElement(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    // =========================================================================
    // Message history
    // =========================================================================

    /**
     * Returns the full message history between the authenticated user and a friend,
     * oldest first (natural chat order), and marks all messages from the friend as read.
     *
     * @param friendId ID of the other participant
     */
    public PageResponse<DtoMessageResponse> getMessages(Long friendId, int page, int size, Authentication auth) {
        User user = principal(auth);
        User friend = findUserOrThrow(friendId);

        EntityConversation conversation = repositoryConversation
                .findByUsers(user.getId(), friend.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No conversation found between you and userId=" + friendId));

        // Mark unread messages from the friend as read.
        repositoryMessage.markAllReadInConversation(conversation.getId(), user.getId());

        Page<EntityMessage> result = repositoryMessage
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId(), PageRequest.of(page, size));

        List<DtoMessageResponse> content = result.getContent()
                .stream()
                .map(this::toMessageDto)
                .toList();

        return PageResponse.<DtoMessageResponse>builder()
                .content(content)
                .number(result.getNumber())
                .size(result.getSize())
                .totalElement(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    // =========================================================================
    // Mark a single message as read
    // =========================================================================

    /**
     * Marks a specific message as read.
     * Only the recipient (not the sender) may call this.
     *
     * @throws EntityNotFoundException        if the message does not exist
     * @throws OperationNotPermittedException if the caller is not the recipient
     */
    public DtoMessageResponse markAsRead(Long messageId, Authentication auth) {
        User user = principal(auth);
        EntityMessage message = repositoryMessage.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        Long recipientId = otherParticipantId(message.getConversation(), message.getSender().getId());
        if (!Objects.equals(recipientId, user.getId())) {
            throw new OperationNotPermittedException("Only the recipient can mark a message as read.");
        }

        message.setRead(true);
        message = repositoryMessage.save(message);

        // Notify the sender that their message was read.
        pushToUser(message.getSender().getId(), EVENT_MESSAGE_READ, toMessageDto(message));

        return toMessageDto(message);
    }

    // =========================================================================
    // Delete a message
    // =========================================================================

    /**
     * Deletes a message.
     * Only the original sender may delete their own message.
     *
     * @throws EntityNotFoundException        if the message does not exist
     * @throws OperationNotPermittedException if the caller is not the sender
     */
    public void deleteMessage(Long messageId, Authentication auth) {
        User user = principal(auth);
        EntityMessage message = repositoryMessage.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!Objects.equals(message.getSender().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can only delete your own messages.");
        }

        repositoryMessage.delete(message);
        log.debug("Message deleted: messageId={} by userId={}", messageId, user.getId());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private User principal(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    private User findUserOrThrow(Long userId) {
        return repositoryUser.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    /** Throws if the two users are not accepted friends. */
    private void ensureFriendship(Long userAId, Long userBId) {
        boolean friends =
                repositoryUserRelation.findByRequesterIdAndAddresseeIdAndRelationType(
                        userAId, userBId, RelationType.FRIEND_REQUEST)
                        .map(r -> r.getStatus() == RelationStatus.ACCEPTED)
                        .orElse(false)
                ||
                repositoryUserRelation.findByRequesterIdAndAddresseeIdAndRelationType(
                        userBId, userAId, RelationType.FRIEND_REQUEST)
                        .map(r -> r.getStatus() == RelationStatus.ACCEPTED)
                        .orElse(false);

        if (!friends) {
            throw new OperationNotPermittedException(
                    "You can only message users who are your friends.");
        }
    }

    /**
     * Creates a new conversation with canonical ID ordering (smaller ID = user1).
     */
    private EntityConversation createConversation(User a, User b) {
        User user1 = a.getId() < b.getId() ? a : b;
        User user2 = a.getId() < b.getId() ? b : a;
        return repositoryConversation.save(
                EntityConversation.builder()
                        .user1(user1)
                        .user2(user2)
                        .build()
        );
    }

    /**
     * Given a conversation and one participant, returns the ID of the other participant.
     */
    private Long otherParticipantId(EntityConversation conversation, Long knownUserId) {
        return Objects.equals(conversation.getUser1().getId(), knownUserId)
                ? conversation.getUser2().getId()
                : conversation.getUser1().getId();
    }

    /** Pushes an SSE event to a user if they are currently connected. */
    private void pushToUser(Long userId, String eventName, Object payload) {
        emitterRegistry.find(userId).ifPresent(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(toJson(payload)));
            } catch (IOException e) {
                log.warn("Failed to push SSE event to userId={}: {}", userId, e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialisation error", e);
            return "{}";
        }
    }

    private DtoMessageResponse toMessageDto(EntityMessage m) {
        return DtoMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .content(m.getContent())
                .replyTo(toReplySnippet(m.getReplyTo()))
                .read(m.isRead())
                .createdAt(m.getCreatedAt())
                .mediaType(m.getMediaType())
                .mediaName(m.getMediaName())
                .mediaSize(m.getMediaSize())
                .hasMedia(m.getMediaData() != null)
                .build();
    }

    /**
     * Builds a compact reply snippet from the referenced message.
     * Returns {@code null} when the message is not a reply.
     * If the original message's sender was deleted, senderName falls back to "Deleted user".
     */
    private DtoMessageReplySnippet toReplySnippet(EntityMessage ref) {
        if (ref == null) return null;
        String senderName = (ref.getSender() != null) ? ref.getSender().getFullName() : "Deleted user";
        Long senderId     = (ref.getSender() != null) ? ref.getSender().getId()       : null;
        return DtoMessageReplySnippet.builder()
                .id(ref.getId())
                .senderId(senderId)
                .senderName(senderName)
                .contentSnippet(truncate(ref.getContent(), 200))
                .build();
    }

    private DtoConversationResponse toConversationDto(EntityConversation c, User caller) {
        // The "friend" is whoever is not the caller in this conversation.
        User friend = Objects.equals(c.getUser1().getId(), caller.getId())
                ? c.getUser2()
                : c.getUser1();

        String preview = repositoryMessage.findLastMessage(c.getId())
                .map(m -> truncate(m.getContent(), 80))
                .orElse(null);

        long unread = repositoryMessage.countUnreadForUser(c.getId(), caller.getId());

        return DtoConversationResponse.builder()
                .conversationId(c.getId())
                .friendId(friend.getId())
                .friendName(friend.getFullName())
                .lastMessagePreview(preview)
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unread)
                .build();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}

