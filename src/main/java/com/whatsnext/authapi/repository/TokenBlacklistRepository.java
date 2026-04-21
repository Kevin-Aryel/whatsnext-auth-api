package com.whatsnext.authapi.repository;

import com.whatsnext.authapi.domain.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {
    boolean existsByTokenHash(String tokenHash);
    void deleteAllByExpiresAtBefore(Instant now);
}
