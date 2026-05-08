package com.whatsnext.authapi.unit.service;

import com.whatsnext.authapi.domain.entity.TokenBlacklist;
import com.whatsnext.authapi.repository.TokenBlacklistRepository;
import com.whatsnext.authapi.service.TokenBlacklistService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceTest {

    @Mock private TokenBlacklistRepository repository;

    private TokenBlacklistService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TokenBlacklistService(repository);
    }

    @Test
    void addToBlacklist_shouldStoreSha256HashNotRawToken() {
        String token = "raw.jwt.token";
        Date expiresAt = Date.from(Instant.now().plusSeconds(60));

        service.addToBlacklist(token, expiresAt);

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(repository).save(captor.capture());
        TokenBlacklist saved = captor.getValue();

        assertThat(saved.getTokenHash())
            .isNotEqualTo(token)
            .hasSize(64) // SHA-256 hex
            .matches("^[0-9a-f]{64}$");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt.toInstant());
    }

    @Test
    void addToBlacklist_sameTokenProducesSameHash() {
        Date expiresAt = Date.from(Instant.now().plusSeconds(60));
        service.addToBlacklist("same-token", expiresAt);
        service.addToBlacklist("same-token", expiresAt);

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getTokenHash())
            .isEqualTo(captor.getAllValues().get(1).getTokenHash());
    }

    @Test
    void addToBlacklist_differentTokensProduceDifferentHashes() {
        Date expiresAt = Date.from(Instant.now().plusSeconds(60));
        service.addToBlacklist("token-a", expiresAt);
        service.addToBlacklist("token-b", expiresAt);

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getTokenHash())
            .isNotEqualTo(captor.getAllValues().get(1).getTokenHash());
    }

    @Test
    void isBlacklisted_whenRepositoryReturnsTrue_returnsTrue() {
        when(repository.existsByTokenHash(anyString())).thenReturn(true);
        assertThat(service.isBlacklisted("any-token")).isTrue();
    }

    @Test
    void isBlacklisted_whenRepositoryReturnsFalse_returnsFalse() {
        when(repository.existsByTokenHash(anyString())).thenReturn(false);
        assertThat(service.isBlacklisted("unknown-token")).isFalse();
    }

    @Test
    void isBlacklisted_queriesByDeterministicHashOfToken() {
        when(repository.existsByTokenHash(anyString())).thenReturn(false);

        service.isBlacklisted("my-token");
        service.isBlacklisted("my-token");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(repository, times(2)).existsByTokenHash(captor.capture());
        assertThat(captor.getAllValues().get(0)).isEqualTo(captor.getAllValues().get(1));
        assertThat(captor.getValue()).matches("^[0-9a-f]{64}$");
    }

    @Test
    void isBlacklisted_lookupReturnsTrueRegardlessOfExpiration() {
        // The repository hit decides; the service does not filter expired entries.
        when(repository.existsByTokenHash(anyString())).thenReturn(true);
        assertThat(service.isBlacklisted("expired-but-still-blacklisted")).isTrue();
    }
}
