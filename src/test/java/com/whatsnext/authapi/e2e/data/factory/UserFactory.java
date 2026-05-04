package com.whatsnext.authapi.e2e.data.factory;

import com.whatsnext.authapi.config.ConfigReader;
import java.util.concurrent.atomic.AtomicInteger;

public class UserFactory {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static UserCredentialRecord validUserCredentials() {
        int id = counter.getAndIncrement();
        return new UserCredentialRecord(
                "Test User",
                "user." + id + "@test.com",
                ConfigReader.get("USER_PASS")
        );
    }
}