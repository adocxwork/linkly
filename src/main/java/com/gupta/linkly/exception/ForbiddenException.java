package com.gupta.linkly.exception;

/** The user is authenticated but not allowed to perform this action (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
