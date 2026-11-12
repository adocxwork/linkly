package com.gupta.linkly.exception;

/** A client mistake whose message is safe to show to the user (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
