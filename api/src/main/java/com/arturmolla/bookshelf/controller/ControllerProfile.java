package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.aspects.annotation.RateLimit;
import com.arturmolla.bookshelf.model.dto.DtoProfile;
import com.arturmolla.bookshelf.model.dto.DtoUpdateProfileRequest;
import com.arturmolla.bookshelf.model.dto.UserDashboardResponse;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.service.ServiceDashboard;
import com.arturmolla.bookshelf.service.ServiceProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("app-profile")
@RequiredArgsConstructor
@Tag(name = "User Profile")
public class ControllerProfile {

    private final ServiceProfile profileService;
    private final ServiceDashboard dashboardService;


    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    public ResponseEntity<DtoProfile> getUserProfile(Authentication connectedUser) {
        return ResponseEntity.ok(profileService.getUserProfile(connectedUser));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile (email cannot be changed)")
    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    public ResponseEntity<DtoProfile> updateUserProfile(
            Authentication connectedUser,
            @RequestBody @Valid DtoUpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateUserProfile(connectedUser, request));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete current user account")
    @RateLimit(capacity = 3, refillTokens = 3, refillDurationMinutes = 1)
    public ResponseEntity<Void> deleteUserProfile(Authentication connectedUser) {
        profileService.deleteUserProfile(connectedUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get user dashboard data")
    @RateLimit(capacity = 10, refillTokens = 10, refillDurationMinutes = 1)
    public ResponseEntity<UserDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetails connectedUser) {
        User user = (User) connectedUser;
        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }
}

