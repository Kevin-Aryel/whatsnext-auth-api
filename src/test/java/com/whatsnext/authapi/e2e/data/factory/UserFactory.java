package com.whatsnext.authapi.e2e.data.factory;

import com.whatsnext.authapi.e2e.config.E2ETestConfig;

import java.util.UUID;

public class UserFactory {

    public static UserCredentialRecord validUserCredentials() {
        String id = UUID.randomUUID().toString();
        return new UserCredentialRecord(
                "Test User",
                "user." + id + "@test.com",
                E2ETestConfig.USER_PASS
        );
    }
}
