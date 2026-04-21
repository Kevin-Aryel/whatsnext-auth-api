package com.whatsnext.authapi.dto.response;

import com.whatsnext.authapi.domain.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String name,
    String email,
    String role,
    Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().name(),
            user.getCreatedAt()
        );
    }
}
