package com.whatsnext.authapi.repository;

import com.whatsnext.authapi.domain.entity.RefreshToken;
import com.whatsnext.authapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteAllByUser(User user);
    List<RefreshToken> findAllByExpiresAtBefore(Instant dateTime);
}
