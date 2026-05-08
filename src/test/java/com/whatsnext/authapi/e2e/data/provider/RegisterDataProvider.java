package com.whatsnext.authapi.e2e.data.provider;

import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import org.testng.annotations.DataProvider;

public class RegisterDataProvider {

    @DataProvider(name = "invalidPasswords")
    public static Object[][] invalidPasswords() {
        return new Object[][] {
                {
                        "sem maiúscula",
                        new UserCredentialRecord("Test User", "user@test.com", "test@1234!"),
                        "Password Too Weak",
                        "Must contain at least one uppercase letter"
                },
                {
                        "sem dígito",
                        new UserCredentialRecord("Test User", "user@test.com", "Test@ABCD!"),
                        "Password Too Weak",
                        "Must contain at least one number"
                },
                {
                        "sem caractere especial",
                        new UserCredentialRecord("Test User", "user@test.com", "Test12345"),
                        "Password Too Weak",
                        "Must contain at least one special character"
                },
                {
                        "menos de 8 caracteres",
                        new UserCredentialRecord("Test User", "user@test.com", "T@1a"),
                        "Validation Error",
                        "Password must be between 8 and 72 characters"
                },
                {
                        "senha vazia",
                        new UserCredentialRecord("Test User", "user@test.com", ""),
                        "Validation Error",
                        "Password must be between 8 and 72 characters"
                },
                {
                        "senha nula",
                        new UserCredentialRecord("Test User", "user@test.com", null),
                        "Validation Error",
                        "Password is required"
                }
        };
    }

    @DataProvider(name = "invalidFields")
    public static Object[][] invalidFields() {
        return new Object[][] {
                {
                        "email inválido",
                        new UserCredentialRecord("Test User", "not-an-email", "Test@1234!"),
                        "Validation Error",
                        "Invalid email format"
                },
                {
                        "email vazio",
                        new UserCredentialRecord("Test User", "", "Test@1234!"),
                        "Validation Error",
                        "Email is required"
                },
                {
                        "email nulo",
                        new UserCredentialRecord("Test User", null, "Test@1234!"),
                        "Validation Error",
                        "Email is required"
                },
                {
                        "nome vazio",
                        new UserCredentialRecord("", "user@test.com", "Test@1234!"),
                        "Validation Error",
                        "Name must be between 2 and 100 characters"
                },
                {
                        "nome menor que 2 caracteres",
                        new UserCredentialRecord("A", "user@test.com", "Test@1234!"),
                        "Validation Error",
                        "Name must be between 2 and 100 characters"
                },
                {
                        "nome nulo",
                        new UserCredentialRecord(null, "user@test.com", "Test@1234!"),
                        "Validation Error",
                        "Name is required"
                }
        };
    }
}