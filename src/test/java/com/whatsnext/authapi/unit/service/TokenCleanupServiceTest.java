package com.whatsnext.authapi.unit.service;

import com.whatsnext.authapi.domain.entity.RefreshToken;
import com.whatsnext.authapi.repository.RefreshTokenRepository;
import com.whatsnext.authapi.repository.TokenBlacklistRepository;
import com.whatsnext.authapi.service.TokenCleanupService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenCleanupServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;

    private TokenCleanupService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TokenCleanupService(refreshTokenRepository, tokenBlacklistRepository);
    }

    @Test
    void cleanExpiredRefreshTokens_findsAndDeletesExpiredEntries() {
        List<RefreshToken> expired = List.of(
            RefreshToken.builder().build(),
            RefreshToken.builder().build()
        );
        Instant before = Instant.now();
        when(refreshTokenRepository.findAllByExpiresAtBefore(any(Instant.class))).thenReturn(expired);

        service.cleanExpiredRefreshTokens();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).findAllByExpiresAtBefore(captor.capture());
        Instant queriedAt = captor.getValue();
        Instant after = Instant.now();

        assertThat(queriedAt).isBetween(before, after);
        verify(refreshTokenRepository).deleteAll(expired);
    }

    @Test
    void cleanExpiredRefreshTokens_whenNoneExpired_stillCallsDeleteAllWithEmptyList() {
        when(refreshTokenRepository.findAllByExpiresAtBefore(any(Instant.class))).thenReturn(List.of());

        service.cleanExpiredRefreshTokens();

        verify(refreshTokenRepository).deleteAll(List.of());
    }

    @Test
    void cleanExpiredBlacklistTokens_callsDeleteAllByExpiresAtBeforeWithCurrentInstant() {
        Instant before = Instant.now();

        service.cleanExpiredBlacklistTokens();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(tokenBlacklistRepository).deleteAllByExpiresAtBefore(captor.capture());
        Instant after = Instant.now();
        assertThat(captor.getValue()).isBetween(before, after);
    }

    @Test
    void cleanupMethods_doNotTouchTheOtherRepository() {
        when(refreshTokenRepository.findAllByExpiresAtBefore(any(Instant.class))).thenReturn(List.of());

        service.cleanExpiredRefreshTokens();
        verifyNoInteractions(tokenBlacklistRepository);

        clearInvocations(refreshTokenRepository);
        service.cleanExpiredBlacklistTokens();
        verifyNoInteractions(refreshTokenRepository);
    }
}
