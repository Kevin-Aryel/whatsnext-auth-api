package com.whatsnext.authapi.exception;

import com.whatsnext.authapi.dto.response.ErrorResponse;
import com.whatsnext.authapi.dto.response.ErrorResponse.ErrorItem;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ErrorResponse.of("EMAIL_ALREADY_EXISTS", "Email Already Exists", e.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(RuntimeException e) {
        return ErrorResponse.of("UNAUTHORIZED", "Unauthorized", "Invalid credentials");
    }

    @ExceptionHandler(TokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleTokenExpired(TokenExpiredException e) {
        return ErrorResponse.of("UNAUTHORIZED", "Unauthorized", "Invalid credentials");
    }

    @ExceptionHandler(PasswordTooWeakException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handlePasswordTooWeak(PasswordTooWeakException e) {
        return ErrorResponse.of("PASSWORD_TOO_WEAK", "Password Too Weak", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        List<ErrorItem> errors = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorItem("VALIDATION_ERROR", "Validation Error", fe.getDefaultMessage()))
            .collect(Collectors.toList());
        return ErrorResponse.ofErrors(errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception e) {
        return ErrorResponse.of("INTERNAL_SERVER_ERROR", "Internal Server Error",
            "An unexpected error occurred");
    }
}
