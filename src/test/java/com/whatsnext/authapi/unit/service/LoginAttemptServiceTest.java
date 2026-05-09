package com.whatsnext.authapi.unit.service;

import com.whatsnext.authapi.service.LoginAttemptService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeMethod
    void setUp() {
        service = new LoginAttemptService(5, 15);
    }

    @Test
    void noAttempts_shouldNotBeLocked() {
        assertThat(service.isLocked("user@example.com")).isFalse();
    }

    @Test
    void belowThreshold_shouldNotBeLocked() {
        for (int i = 0; i < 4; i++) service.recordFailure("user@example.com");
        assertThat(service.isLocked("user@example.com")).isFalse();
    }

    @Test
    void atThreshold_shouldBeLocked() {
        for (int i = 0; i < 5; i++) service.recordFailure("user@example.com");
        assertThat(service.isLocked("user@example.com")).isTrue();
    }

    @Test
    void successAfterFailures_shouldUnlock() {
        for (int i = 0; i < 5; i++) service.recordFailure("user@example.com");
        service.recordSuccess("user@example.com");
        assertThat(service.isLocked("user@example.com")).isFalse();
    }

    @Test
    void differentEmails_shouldHaveIndependentCounters() {
        for (int i = 0; i < 5; i++) service.recordFailure("a@example.com");
        assertThat(service.isLocked("b@example.com")).isFalse();
    }

    @Test
    void lockExpiresAfterDuration_shouldUnlock() {
        // 0 lock-minutes means the lock window is already past as soon as it's set.
        LoginAttemptService instantUnlock = new LoginAttemptService(2, 0);
        instantUnlock.recordFailure("user@example.com");
        instantUnlock.recordFailure("user@example.com");
        assertThat(instantUnlock.isLocked("user@example.com")).isFalse();
    }
}
