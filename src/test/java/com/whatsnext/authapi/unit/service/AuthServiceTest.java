package com.whatsnext.authapi.unit.service;

import com.whatsnext.authapi.config.JwtConfig;
import com.whatsnext.authapi.domain.entity.RefreshToken;
import com.whatsnext.authapi.domain.entity.User;
import com.whatsnext.authapi.domain.enums.Role;
import com.whatsnext.authapi.dto.request.LoginRequest;
import com.whatsnext.authapi.dto.request.RefreshRequest;
import com.whatsnext.authapi.dto.request.RegisterRequest;
import com.whatsnext.authapi.dto.response.AuthResponse;
import com.whatsnext.authapi.exception.*;
import com.whatsnext.authapi.repository.RefreshTokenRepository;
import com.whatsnext.authapi.repository.UserRepository;
import com.whatsnext.authapi.service.AuthService;
import com.whatsnext.authapi.service.JwtService;
import com.whatsnext.authapi.service.PasswordValidator;
import com.whatsnext.authapi.service.TokenBlacklistService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;

    private AuthService authService;
    private BCryptPasswordEncoder encoder;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);

        JwtConfig config = new JwtConfig();
        config.setSecret("dGVzdFNlY3JldEtleUZvclVuaXRUZXN0czEyMzQ1Njc4OTAxMjM0NTY=");
        config.setAccessExpiration(900L);
        config.setRefreshExpiration(604800L);

        JwtService jwtService = new JwtService(config);
        PasswordValidator passwordValidator = new PasswordValidator();
        encoder = new BCryptPasswordEncoder(12);

        authService = new AuthService(
            userRepository, refreshTokenRepository,
            tokenBlacklistService, jwtService,
            passwordValidator, encoder, config
        );
    }

    @Test
    void register_withNewEmail_shouldReturnTokens() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(
            new RegisterRequest("Test User", "new@example.com", "Secure@123")
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void register_withDuplicateEmail_shouldThrowEmailAlreadyExistsException() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("Test", "dup@example.com", "Secure@123")))
            .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void register_withWeakPassword_shouldThrowPasswordTooWeakException() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("Test", "x@example.com", "weak")))
            .isInstanceOf(PasswordTooWeakException.class);
    }

    @Test
    void login_withValidCredentials_shouldReturnTokens() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .passwordHash(encoder.encode("Secure@123"))
            .role(Role.USER)
            .name("User")
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(
            new LoginRequest("user@example.com", "Secure@123")
        );

        assertThat(response.accessToken()).isNotBlank();
    }

    @Test
    void login_withUnknownEmail_shouldThrowInvalidTokenException() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("ghost@example.com", "Secure@123")))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    void login_withWrongPassword_shouldThrowInvalidTokenException() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .passwordHash(encoder.encode("Correct@123"))
            .role(Role.USER)
            .name("User")
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("user@example.com", "Wrong@123")))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    void refresh_withUsedToken_shouldThrowInvalidTokenException() {
        RefreshToken usedToken = RefreshToken.builder()
            .token("used-token")
            .used(true)
            .expiresAt(Instant.now().plusSeconds(3600))
            .user(User.builder().id(UUID.randomUUID()).build())
            .build();

        when(refreshTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("used-token")))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_withExpiredToken_shouldThrowTokenExpiredException() {
        RefreshToken expiredToken = RefreshToken.builder()
            .token("expired-token")
            .used(false)
            .expiresAt(Instant.now().minusSeconds(1))
            .user(User.builder().id(UUID.randomUUID()).build())
            .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token")))
            .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void refresh_shouldDeleteOldTokenAndNotMarkUsed() {
        UUID tokenId = UUID.randomUUID();
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("rotate@example.com")
            .role(Role.USER)
            .name("User")
            .build();
        RefreshToken existing = RefreshToken.builder()
            .id(tokenId)
            .token("rotate-token")
            .used(false)
            .expiresAt(Instant.now().plusSeconds(3600))
            .user(user)
            .build();

        when(refreshTokenRepository.findByToken("rotate-token")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.refresh(new RefreshRequest("rotate-token"));

        // Old token must be deleted atomically — never re-saved with used=true.
        verify(refreshTokenRepository).deleteById(tokenId);
        verify(refreshTokenRepository, never()).save(existing);
    }
}
