package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.aspects.annotation.RateLimit;
import com.arturmolla.bookshelf.model.dto.DtoProfile;
import com.arturmolla.bookshelf.model.dto.UserDashboardResponse;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.service.ServiceDashboard;
import com.arturmolla.bookshelf.service.ServiceProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
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
    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    public ResponseEntity<DtoProfile> getUserProfile(Authentication connectedUser) {
        return ResponseEntity.ok(profileService.getUserProfile(connectedUser));
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

