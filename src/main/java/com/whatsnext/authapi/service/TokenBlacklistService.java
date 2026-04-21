package com.whatsnext.authapi.service;

import com.whatsnext.authapi.domain.entity.TokenBlacklist;
import com.whatsnext.authapi.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository repository;

    public void addToBlacklist(String token, Date expiresAt) {
        String hash = sha256(token);
        TokenBlacklist entry = TokenBlacklist.builder()
                .tokenHash(hash)
                .expiresAt(expiresAt.toInstant())
                .build();
        repository.save(entry);
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByTokenHash(sha256(token));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
