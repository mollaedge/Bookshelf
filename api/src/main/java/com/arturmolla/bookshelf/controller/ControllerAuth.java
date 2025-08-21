package com.arturmolla.bookshelf.controller;

import com.arturmolla.bookshelf.aspects.annotation.RateLimit;
import com.arturmolla.bookshelf.model.dto.AuthenticationRequest;
import com.arturmolla.bookshelf.model.dto.AuthenticationResponse;
import com.arturmolla.bookshelf.model.dto.DtoRegistrationRequest;
import com.arturmolla.bookshelf.service.ServiceAuthentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class ControllerAuth {

    private final ServiceAuthentication service;

    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> register(@RequestBody @Valid DtoRegistrationRequest request) throws MessagingException {
        service.registerUser(request);
        return ResponseEntity.accepted().build();
    }

    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest authenticationRequest) {
        return ResponseEntity.ok(service.authenticate(authenticationRequest));
    }

    @RateLimit(capacity = 5, refillTokens = 5, refillDurationMinutes = 1)
    @GetMapping("/activate-account")
    public void confirm(@RequestParam String token, @RequestParam String email) throws MessagingException {
        service.activateAccount(token, email);
    }
}
