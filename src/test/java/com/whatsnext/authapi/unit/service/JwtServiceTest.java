package com.whatsnext.authapi.unit.service;

import com.whatsnext.authapi.config.JwtConfig;
import com.whatsnext.authapi.domain.entity.User;
import com.whatsnext.authapi.domain.enums.Role;
import com.whatsnext.authapi.exception.InvalidTokenException;
import com.whatsnext.authapi.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("dGVzdFNlY3JldEtleUZvclVuaXRUZXN0czEyMzQ1Njc4OTAxMjM0NTY=");
        config.setAccessExpiration(900L);
        config.setRefreshExpiration(604800L);
        jwtService = new JwtService(config);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .passwordHash("irrelevant")
                .build();
    }

    @Test
    void generateAccessToken_shouldContainEmailAsSubject() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractSubject(token)).isEqualTo("test@example.com");
    }

    @Test
    void generateAccessToken_shouldBeValidForSameUser() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void generateAccessToken_shouldBeInvalidForDifferentUser() {
        String token = jwtService.generateAccessToken(user);
        User other = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .name("Other")
                .role(Role.USER)
                .passwordHash("x")
                .build();
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void generateRefreshToken_shouldReturnNonNullUniqueValues() {
        String t1 = jwtService.generateRefreshToken();
        String t2 = jwtService.generateRefreshToken();
        assertThat(t1).isNotNull().isNotEqualTo(t2);
    }

    @Test
    void isTokenExpired_withFreshToken_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void extractSubject_withTamperedToken_shouldThrowInvalidTokenException() {
        String token = jwtService.generateAccessToken(user) + "tampered";
        assertThatThrownBy(() -> jwtService.extractSubject(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void extractSubject_withMalformedToken_shouldThrowInvalidTokenException() {
        assertThatThrownBy(() -> jwtService.extractSubject("not.a.jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
