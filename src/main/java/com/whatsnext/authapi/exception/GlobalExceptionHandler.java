package com.whatsnext.authapi.exception;

import com.whatsnext.authapi.dto.response.ErrorResponse;
import com.whatsnext.authapi.dto.response.ErrorResponse.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static String code(HttpStatus status) {
        return String.valueOf(status.value());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ErrorResponse.of(code(HttpStatus.CONFLICT), "Email Already Exists", e.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(RuntimeException e) {
        return ErrorResponse.of(code(HttpStatus.UNAUTHORIZED), "Unauthorized", "Invalid credentials");
    }

    @ExceptionHandler(TokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleTokenExpired(TokenExpiredException e) {
        return ErrorResponse.of(code(HttpStatus.UNAUTHORIZED), "Unauthorized", "Invalid credentials");
    }

    @ExceptionHandler(PasswordTooWeakException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handlePasswordTooWeak(PasswordTooWeakException e) {
        return ErrorResponse.of(code(HttpStatus.UNPROCESSABLE_ENTITY), "Password Too Weak", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        String statusCode = code(HttpStatus.UNPROCESSABLE_ENTITY);
        List<ErrorItem> errors = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorItem(statusCode, "Validation Error", fe.getDefaultMessage()))
            .collect(Collectors.toList());
        return ErrorResponse.ofErrors(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException e) {
        return ErrorResponse.of(code(HttpStatus.BAD_REQUEST), "Bad Request", "Malformed or missing request body");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingHeader(MissingRequestHeaderException e) {
        return ErrorResponse.of(code(HttpStatus.BAD_REQUEST), "Bad Request",
            "Missing required header: " + e.getHeaderName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException e) {
        return ErrorResponse.of(code(HttpStatus.BAD_REQUEST), "Bad Request",
            "Missing required parameter: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ErrorResponse.of(code(HttpStatus.BAD_REQUEST), "Bad Request",
            "Invalid value for parameter: " + e.getName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ErrorResponse.of(code(HttpStatus.METHOD_NOT_ALLOWED), "Method Not Allowed",
            "HTTP method not supported for this endpoint");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NoResourceFoundException e) {
        return ErrorResponse.of(code(HttpStatus.NOT_FOUND), "Not Found", "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception e) {
        log.error("Unhandled exception reached generic handler", e);
        return ErrorResponse.of(code(HttpStatus.INTERNAL_SERVER_ERROR), "Internal Server Error",
            "An unexpected error occurred");
    }
}
