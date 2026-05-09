package com.whatsnext.authapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private record AttemptRecord(int count, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final int lockMinutes;

    public LoginAttemptService(
            @Value("${login-attempt.max-attempts:5}") int maxAttempts,
            @Value("${login-attempt.lock-minutes:15}") int lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    public boolean isLocked(String email) {
        AttemptRecord record = attempts.get(email);
        if (record == null || record.lockedUntil() == null) return false;
        if (Instant.now().isBefore(record.lockedUntil())) return true;
        // Lock window elapsed — clear so the user starts fresh.
        attempts.remove(email);
        return false;
    }

    public void recordFailure(String email) {
        attempts.merge(email,
                new AttemptRecord(1, null),
                (existing, ignored) -> {
                    int newCount = existing.count() + 1;
                    Instant lockUntil = newCount >= maxAttempts
                            ? Instant.now().plusSeconds(lockMinutes * 60L)
                            : null;
                    return new AttemptRecord(newCount, lockUntil);
                });
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }
}
