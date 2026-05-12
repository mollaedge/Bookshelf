package com.arturmolla.bookshelf.controller.admin;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.DtoAdminUpdateUserRequest;
import com.arturmolla.bookshelf.model.dto.DtoAdminUserResponse;
import com.arturmolla.bookshelf.service.ServiceAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin – User Management")
public class ControllerAdminUsers {

    private final ServiceAdmin adminService;

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    @GetMapping("/users")
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<PageResponse<DtoAdminUserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<DtoAdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    @PutMapping("/users/{id}")
    @Operation(summary = "Update a user's profile fields, lock status or enabled status")
    public ResponseEntity<DtoAdminUserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid DtoAdminUpdateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    // -----------------------------------------------------------------------
    // Activate / Deactivate
    // -----------------------------------------------------------------------

    @PatchMapping("/users/{id}/activate")
    @Operation(summary = "Activate a user account (set enabled = true)")
    public ResponseEntity<DtoAdminUserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.activateUser(id));
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(summary = "Deactivate a user account (set enabled = false)")
    public ResponseEntity<DtoAdminUserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateUser(id));
    }

    // -----------------------------------------------------------------------
    // Lock / Unlock
    // -----------------------------------------------------------------------

    @PatchMapping("/users/{id}/lock")
    @Operation(summary = "Lock a user account")
    public ResponseEntity<DtoAdminUserResponse> lockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.lockUser(id));
    }

    @PatchMapping("/users/{id}/unlock")
    @Operation(summary = "Unlock a user account")
    public ResponseEntity<DtoAdminUserResponse> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockUser(id));
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user by ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

