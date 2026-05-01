package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.dto.AuthenticationRequest;
import com.arturmolla.bookshelf.model.dto.DtoToken;
import com.arturmolla.bookshelf.model.dto.DtoRegistrationRequest;
import com.arturmolla.bookshelf.model.enums.EmailTemplateName;
import com.arturmolla.bookshelf.model.user.Token;
import com.arturmolla.bookshelf.model.user.User;
import com.arturmolla.bookshelf.repository.RepositoryRole;
import com.arturmolla.bookshelf.repository.RepositoryToken;
import com.arturmolla.bookshelf.repository.RepositoryUser;
import com.arturmolla.bookshelf.security.JwtService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAuthentication {

    private final RepositoryRole repositoryRole;
    private final PasswordEncoder passwordEncoder;
    private final RepositoryUser repositoryUser;
    private final RepositoryToken repositoryToken;
    private final ServiceEmail serviceEmail;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;
    @Value("${application.mailing.frontend.login-url}")
    private String loginUrl;

    public void registerUser(DtoRegistrationRequest request) throws MessagingException {
        var userRole = repositoryRole.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER not found/initialized"));
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .provider("EMAIL_PASSWORD")
                .build();
        repositoryUser.save(user);
        sendVerificationEmail(user);
    }

    public DtoToken authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var claims = new HashMap<String, Object>();
        var user = (User) auth.getPrincipal();
        claims.put("fullName", user.getFullName());
        var jwtToken = jwtService.generateToken(claims, user);
        return new DtoToken(jwtToken);
    }

    public void activateAccount(String token, String email) throws MessagingException {
        Token savedToken = repositoryToken.findByTokenAndUserEmail(token, email)
                .orElseThrow(() -> new RuntimeException("Invalid Token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            repositoryToken.delete(savedToken);
            this.generateAndSaveActivationToken(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been send");
        }
        sendWelcomeEmail(savedToken.getUser());
        var user = repositoryUser.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found!"));
        user.setEnabled(true);
        repositoryUser.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        repositoryToken.save(savedToken);
    }

    private void sendWelcomeEmail(User user) throws MessagingException {
        serviceEmail.sendWelcomeEmail(user.getEmail(), user.getFullName(), EmailTemplateName.WELCOME_MESSAGE,
                loginUrl, "Account activated!");
    }

    private void sendVerificationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        serviceEmail.sendEmail(user.getEmail(), user.getFullName(), EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl, newToken, "Account activation!");
    }

    private String generateAndSaveActivationToken(User user) {
        String generatedToken = generateActivationCode();
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        repositoryToken.save(token);
        return generatedToken;
    }

    private String generateActivationCode() {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < 6; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }
}
