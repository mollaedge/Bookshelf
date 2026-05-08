package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoNotificationResponse;
import com.arturmolla.bookshelf.service.ServiceNotification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("notifications")
@Tag(name = "Notifications")
public class ControllerNotification {

    private final ServiceNotification serviceNotification;

    // ─────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns the authenticated user's notifications, newest first, paged.
     * Each item contains type, title, message, read status, actor info and referenceId/Type.
     */
    @GetMapping
    @Operation(summary = "Get my notifications (newest first, paged)")
    public ResponseEntity<PageResponse<DtoNotificationResponse>> getMyNotifications(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "20", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceNotification.getMyNotifications(page, size, connectedUser));
    }

    /**
     * Returns the count of unread notifications for the authenticated user.
     * Ideal for polling the notification bell badge.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get the number of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication connectedUser) {
        long count = serviceNotification.getUnreadCount(connectedUser);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE (mark as read)
    // ─────────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read.
     * Returns the updated notification.
     */
    @PatchMapping("/{notification-id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<DtoNotificationResponse> markAsRead(
            @PathVariable("notification-id") Long notificationId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(serviceNotification.markAsRead(notificationId, connectedUser));
    }

    /**
     * Marks ALL notifications of the authenticated user as read in one call.
     */
    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(Authentication connectedUser) {
        serviceNotification.markAllAsRead(connectedUser);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes a single notification. Only the recipient can delete it.
     */
    @DeleteMapping("/{notification-id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("notification-id") Long notificationId,
            Authentication connectedUser
    ) {
        serviceNotification.deleteNotification(notificationId, connectedUser);
        return ResponseEntity.noContent().build();
    }
}

