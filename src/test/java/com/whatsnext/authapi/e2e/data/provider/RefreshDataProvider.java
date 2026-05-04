package com.whatsnext.authapi.e2e.data.provider;

import org.testng.annotations.DataProvider;

public class RefreshDataProvider {

    @DataProvider(name = "invalidRefreshTokens")
    public static Object[][] invalidRefreshTokens() {
        return new Object[][] {
                { "not-a-valid-uuid",                    "Invalid credentials" },
                { "00000000-0000-0000-0000-000000000000", "Invalid credentials" }
        };
    }
}