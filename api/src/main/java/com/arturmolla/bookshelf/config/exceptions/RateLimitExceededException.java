package com.arturmolla.bookshelf.config.exceptions;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many requests. Please slow down and try again later.");
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}

