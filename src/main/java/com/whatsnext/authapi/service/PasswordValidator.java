package com.whatsnext.authapi.service;

import com.whatsnext.authapi.exception.PasswordTooWeakException;
import org.springframework.stereotype.Service;

@Service
public class PasswordValidator {

    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new PasswordTooWeakException("must have at least 8 characters");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new PasswordTooWeakException("must contain at least one uppercase letter");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new PasswordTooWeakException("must contain at least one number");
        }
        if (!password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            throw new PasswordTooWeakException("must contain at least one special character");
        }
    }
}
