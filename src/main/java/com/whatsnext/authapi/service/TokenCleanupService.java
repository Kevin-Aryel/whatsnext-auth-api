package com.whatsnext.authapi.service;

import com.whatsnext.authapi.repository.RefreshTokenRepository;
import com.whatsnext.authapi.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    // Runs at 03:00 UTC daily. On Render free tier, only executes if instance is awake.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredRefreshTokens() {
        var expired = refreshTokenRepository.findAllByExpiresAtBefore(Instant.now());
        refreshTokenRepository.deleteAll(expired);
        log.info("Cleaned {} expired refresh tokens", expired.size());
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanExpiredBlacklistTokens() {
        tokenBlacklistRepository.deleteAllByExpiresAtBefore(Instant.now());
        log.info("Cleaned expired blacklist entries");
    }
}
