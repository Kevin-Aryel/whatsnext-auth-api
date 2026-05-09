package com.whatsnext.authapi.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        passwordValidator.validate(request.password());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        return generateTokenPair(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidTokenException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidTokenException("Invalid credentials");
        }

        return generateTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken existing = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (existing.isUsed()) {
            throw new InvalidTokenException("Refresh token already used");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException();
        }

        // Delete instead of marking used: deleteById is atomic at the DB level, so a
        // second concurrent refresh races into "row not found" -> InvalidTokenException
        // instead of both seeing isUsed()==false and rotating twice off the same token.
        User user = existing.getUser();
        refreshTokenRepository.deleteById(existing.getId());

        return generateTokenPair(user);
    }

    @Transactional
    public void logout(String accessToken, String refreshTokenValue) {
        Date expiration = jwtService.extractExpiration(accessToken);
        tokenBlacklistService.addToBlacklist(accessToken, expiration);

        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(rt -> {
                rt.setUsed(true);
                refreshTokenRepository.save(rt);
            });
        }
    }

    private AuthResponse generateTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(jwtConfig.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenValue, jwtConfig.getAccessExpiration());
    }
}
