package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.AppFeedbackDto;
import com.arturmolla.bookshelf.model.dto.AppFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.CommentDto;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.service.AppFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("app-feedbacks")
@RequiredArgsConstructor
@Tag(name = "Application Feedback")
public class ControllerAppFeedback {

    private final AppFeedbackService appFeedbackService;

    @PostMapping
    @Operation(summary = "Submit new app feedback")
    public ResponseEntity<AppFeedbackDto> save(
            @Valid @RequestBody AppFeedbackRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.save(request, connectedUser));
    }

    @GetMapping
    @Operation(summary = "Get all app feedbacks (paginated)")
    public ResponseEntity<PageResponse<AppFeedbackDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.getAll(page, size, connectedUser));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my own feedbacks (paginated)")
    public ResponseEntity<PageResponse<AppFeedbackDto>> getMyFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.getMyFeedbacks(page, size, connectedUser));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single feedback by ID")
    public ResponseEntity<AppFeedbackDto> getById(
            @PathVariable Long id,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.getById(id, connectedUser));
    }

    @PatchMapping("/{id}/upvote")
    @Operation(summary = "Toggle upvote on a feedback")
    public ResponseEntity<AppFeedbackDto> upvote(
            @PathVariable Long id,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.upvote(id, connectedUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a feedback (owner or admin)")
    public ResponseEntity<AppFeedbackDto> edit(
            @PathVariable Long id,
            @Valid @RequestBody AppFeedbackRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.edit(id, request, connectedUser));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a feedback")
    public ResponseEntity<AppFeedbackDto> addComment(
            @PathVariable Long id,
            @RequestBody CommentDto commentDto,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.addComment(id, commentDto, connectedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a feedback (owner or admin)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication connectedUser
    ) {
        appFeedbackService.delete(id, connectedUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Change the status of a feedback (admin only)")
    public ResponseEntity<AppFeedbackDto> changeStatus(
            @PathVariable Long id,
            @RequestParam AppFeedbackStatus status,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.changeStatus(id, status, connectedUser));
    }

    @GetMapping("/by-status")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Get all feedbacks filtered by status (admin only)")
    public ResponseEntity<PageResponse<AppFeedbackDto>> getAllByStatus(
            @RequestParam AppFeedbackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(appFeedbackService.getAllByStatus(status, page, size, connectedUser));
    }
}
