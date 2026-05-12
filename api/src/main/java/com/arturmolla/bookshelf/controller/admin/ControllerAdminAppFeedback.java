package com.arturmolla.bookshelf.controller.admin;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.AppFeedbackRequest;
import com.arturmolla.bookshelf.model.dto.DtoAdminFeedbackUpdateRequest;
import com.arturmolla.bookshelf.model.dto.DtoAdminSystemCommentRequest;
import com.arturmolla.bookshelf.model.dto.DtoAppFeedback;
import com.arturmolla.bookshelf.model.enums.AppFeedbackStatus;
import com.arturmolla.bookshelf.service.ServiceAdminAppFeedback;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/feedbacks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin – App Feedback Management")
public class ControllerAdminAppFeedback {

    private final ServiceAdminAppFeedback adminFeedbackService;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Get all app feedbacks (paginated)")
    public ResponseEntity<PageResponse<DtoAppFeedback>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(adminFeedbackService.getAll(page, size));
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get all app feedbacks filtered by status")
    public ResponseEntity<PageResponse<DtoAppFeedback>> getAllByStatus(
            @RequestParam AppFeedbackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(adminFeedbackService.getAllByStatus(status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single app feedback by ID")
    public ResponseEntity<DtoAppFeedback> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminFeedbackService.getById(id));
    }

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Create a new app feedback as admin")
    public ResponseEntity<DtoAppFeedback> create(
            @Valid @RequestBody AppFeedbackRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(adminFeedbackService.create(request, connectedUser));
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    @PutMapping("/{id}")
    @Operation(summary = "Update a feedback's title, description and/or status")
    public ResponseEntity<DtoAppFeedback> update(
            @PathVariable Long id,
            @Valid @RequestBody DtoAdminFeedbackUpdateRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(adminFeedbackService.update(id, request, connectedUser));
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an app feedback")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminFeedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // System comments
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a system comment to a feedback (shown as 'System', authorId = null)")
    public ResponseEntity<DtoAppFeedback> addSystemComment(
            @PathVariable Long id,
            @Valid @RequestBody DtoAdminSystemCommentRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(adminFeedbackService.addSystemComment(id, request, connectedUser));
    }

    @DeleteMapping("/{feedbackId}/comments/{index}")
    @Operation(summary = "Delete a comment by its zero-based index in the feedback's comment list")
    public ResponseEntity<DtoAppFeedback> deleteComment(
            @PathVariable Long feedbackId,
            @PathVariable int index,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(adminFeedbackService.deleteComment(feedbackId, index, connectedUser));
    }
}

