package com.arturmolla.bookshelf.model.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum BusinessErrorCodes {

    NO_CODE(0, HttpStatus.NOT_IMPLEMENTED, "No code"),
    ACCOUNT_LOCKED(302, HttpStatus.FORBIDDEN, "User account locked"),
    INCORRECT_CURRENT_PASSWORD(300, HttpStatus.BAD_REQUEST, "Current password incorrect"),
    NEW_PASSWORD_DOES_NOT_MATCH(301, HttpStatus.BAD_REQUEST, "New password does not match"),
    ACCOUNT_DISABLED(303, HttpStatus.FORBIDDEN, "User account disabled"),
    BAD_CREDENTIALS(304, HttpStatus.FORBIDDEN, "Login and / or password is incorrect"),
    JWT_EXPIRED(305, HttpStatus.UNAUTHORIZED, "JWT token has expired"),
    RATE_LIMIT_EXCEEDED(429, HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please slow down and try again later.");

    @Getter
    private final int code;
    @Getter
    private final String description;
    @Getter
    private final HttpStatus status;

    BusinessErrorCodes(int code, HttpStatus status, String description) {
        this.code = code;
        this.description = description;
        this.status = status;
    }
}
