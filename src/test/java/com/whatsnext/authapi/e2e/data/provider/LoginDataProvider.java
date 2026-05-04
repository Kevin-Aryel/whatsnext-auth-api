package com.whatsnext.authapi.e2e.data.provider;

import org.testng.annotations.DataProvider;

public class LoginDataProvider {
    @DataProvider(name = "invalidLoginFields")
    public static Object[][] invalidLoginFields() {
        return new Object[][] {
                { "not-an-email",                 "Test@1234!", "Invalid email format" },
                { "",                             "Test@1234!", "Email is required"    },
                { null,                           "Test@1234!", "Email is required"    },
                { "user-empty-password@test.com", "",           "Password is required" },
                { "user-empty-password@test.com", null,         "Password is required" }
        };
    }
}