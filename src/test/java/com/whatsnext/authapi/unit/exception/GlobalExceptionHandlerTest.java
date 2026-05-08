package com.whatsnext.authapi.unit.exception;

import com.whatsnext.authapi.dto.response.ErrorResponse;
import com.whatsnext.authapi.exception.EmailAlreadyExistsException;
import com.whatsnext.authapi.exception.GlobalExceptionHandler;
import com.whatsnext.authapi.exception.InvalidTokenException;
import com.whatsnext.authapi.exception.PasswordTooWeakException;
import com.whatsnext.authapi.exception.TokenExpiredException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeMethod
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleEmailAlreadyExists_returns409() {
        ErrorResponse resp = handler.handleEmailAlreadyExists(
            new EmailAlreadyExistsException("user@example.com"));

        assertThat(resp.errors()).hasSize(1);
        assertThat(resp.errors().get(0).code()).isEqualTo("409");
        assertThat(resp.errors().get(0).title()).isEqualTo("Email Already Exists");
        assertThat(resp.errors().get(0).detail()).contains("user@example.com");
        assertThat(resp.meta()).isNotNull();
    }

    @Test
    void handleUnauthorized_invalidToken_returnsGenericInvalidCredentials() {
        ErrorResponse resp = handler.handleUnauthorized(new InvalidTokenException("real cause"));

        assertThat(resp.errors().get(0).code()).isEqualTo("401");
        assertThat(resp.errors().get(0).title()).isEqualTo("Unauthorized");
        assertThat(resp.errors().get(0).detail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleUnauthorized_usernameNotFound_returnsGenericInvalidCredentials() {
        ErrorResponse resp = handler.handleUnauthorized(new UsernameNotFoundException("user@x.com"));

        assertThat(resp.errors().get(0).code()).isEqualTo("401");
        assertThat(resp.errors().get(0).detail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleTokenExpired_returns401WithGenericMessage() {
        ErrorResponse resp = handler.handleTokenExpired(new TokenExpiredException());

        assertThat(resp.errors().get(0).code()).isEqualTo("401");
        assertThat(resp.errors().get(0).title()).isEqualTo("Unauthorized");
        assertThat(resp.errors().get(0).detail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handlePasswordTooWeak_returns422() {
        ErrorResponse resp = handler.handlePasswordTooWeak(
            new PasswordTooWeakException("needs uppercase"));

        assertThat(resp.errors().get(0).code()).isEqualTo("422");
        assertThat(resp.errors().get(0).title()).isEqualTo("Password Too Weak");
        assertThat(resp.errors().get(0).detail()).contains("needs uppercase");
    }

    @Test
    void handleValidation_returnsAllFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(
            new FieldError("obj", "email", "must be valid"),
            new FieldError("obj", "password", "must not be blank")
        ));

        ErrorResponse resp = handler.handleValidation(ex);

        assertThat(resp.errors()).hasSize(2);
        assertThat(resp.errors()).allSatisfy(item -> {
            assertThat(item.code()).isEqualTo("422");
            assertThat(item.title()).isEqualTo("Validation Error");
        });
        assertThat(resp.errors()).extracting(ErrorResponse.ErrorItem::detail)
            .containsExactlyInAnyOrder("must be valid", "must not be blank");
    }

    @Test
    void handleUnreadableBody_returns400() {
        ErrorResponse resp = handler.handleUnreadableBody(
            new HttpMessageNotReadableException("bad json"));

        assertThat(resp.errors().get(0).code()).isEqualTo("400");
        assertThat(resp.errors().get(0).title()).isEqualTo("Bad Request");
        assertThat(resp.errors().get(0).detail()).contains("Malformed");
    }

    @Test
    void handleMissingHeader_includesHeaderName() throws Exception {
        MissingRequestHeaderException ex = new MissingRequestHeaderException(
            "X-Trace-Id",
            new org.springframework.core.MethodParameter(
                Object.class.getMethod("toString"), -1));

        ErrorResponse resp = handler.handleMissingHeader(ex);

        assertThat(resp.errors().get(0).code()).isEqualTo("400");
        assertThat(resp.errors().get(0).detail()).contains("X-Trace-Id");
    }

    @Test
    void handleMissingParameter_includesParameterName() {
        MissingServletRequestParameterException ex =
            new MissingServletRequestParameterException("page", "int");

        ErrorResponse resp = handler.handleMissingParameter(ex);

        assertThat(resp.errors().get(0).code()).isEqualTo("400");
        assertThat(resp.errors().get(0).detail()).contains("page");
    }

    @Test
    void handleTypeMismatch_includesParameterName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        ErrorResponse resp = handler.handleTypeMismatch(ex);

        assertThat(resp.errors().get(0).code()).isEqualTo("400");
        assertThat(resp.errors().get(0).detail()).contains("id");
    }

    @Test
    void handleMethodNotAllowed_returns405() {
        ErrorResponse resp = handler.handleMethodNotAllowed(
            new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(resp.errors().get(0).code()).isEqualTo("405");
        assertThat(resp.errors().get(0).title()).isEqualTo("Method Not Allowed");
    }

    @Test
    void handleNotFound_returns404() {
        ErrorResponse resp = handler.handleNotFound(
            mock(NoResourceFoundException.class));

        assertThat(resp.errors().get(0).code()).isEqualTo("404");
        assertThat(resp.errors().get(0).title()).isEqualTo("Not Found");
        assertThat(resp.errors().get(0).detail()).isEqualTo("Resource not found");
    }

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        ErrorResponse resp = handler.handleGeneric(new RuntimeException("ohno"));

        assertThat(resp.errors().get(0).code()).isEqualTo("500");
        assertThat(resp.errors().get(0).title()).isEqualTo("Internal Server Error");
        assertThat(resp.errors().get(0).detail()).isEqualTo("An unexpected error occurred");
    }
}
