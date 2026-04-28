package com.whatsnext.authapi.e2e.data.provider;

import com.whatsnext.authapi.e2e.data.factory.UserDbRecord;
import org.testng.annotations.DataProvider;

public class RegisterDataProvider {

    @DataProvider(name = "invalidPasswords")
    public static Object[][] invalidPasswords() {
        return new Object[][] {
                { "sem maiúscula",          new UserDbRecord("Test User", "user@test.com", "test@1234!")  },
                { "sem dígito",             new UserDbRecord("Test User", "user@test.com", "Test@ABCD!")  },
                { "sem caractere especial", new UserDbRecord("Test User", "user@test.com", "Test12345")   },
                { "menos de 8 caracteres", new UserDbRecord("Test User", "user@test.com", "T@1a")         }
        };
    }

    @DataProvider(name = "invalidFields")
    public static Object[][] invalidFields() {
        return new Object[][] {
                { "email inválido",              new UserDbRecord("Test User", "not-an-email", "Test@1234!") },
                { "nome vazio",                  new UserDbRecord("",          "user@test.com", "Test@1234!") },
                { "nome menor que 2 caracteres", new UserDbRecord("A",         "user@test.com", "Test@1234!") }
        };
    }
}