package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.aspects.annotation.RateLimit;
import com.arturmolla.bookshelf.model.dto.DtoProfile;
import com.arturmolla.bookshelf.service.ServiceProfile;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("app-profile")
@RequiredArgsConstructor
@Tag(name = "User Profile")
public class ControllerProfile {

    private final ServiceProfile profileService;


    @GetMapping("/profile")
    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    public ResponseEntity<DtoProfile> getUserProfile(Authentication connectedUser) {
        return ResponseEntity.ok(profileService.getUserProfile(connectedUser));
    }
}
