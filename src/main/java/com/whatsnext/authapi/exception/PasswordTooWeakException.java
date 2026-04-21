package com.whatsnext.authapi.exception;

public class PasswordTooWeakException extends RuntimeException {
    public PasswordTooWeakException(String reason) {
        super("Password too weak: " + reason);
    }
}
