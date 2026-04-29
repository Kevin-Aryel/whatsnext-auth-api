package com.whatsnext.authapi.unit.service;

import com.whatsnext.authapi.exception.PasswordTooWeakException;
import com.whatsnext.authapi.service.PasswordValidator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.*;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void validate_withValidPassword_shouldNotThrow() {
        assertThatNoException().isThrownBy(() -> validator.validate("Secure@123"));
    }

    @DataProvider(name = "shortPasswords")
    public Object[][] shortPasswords() {
        return new Object[][]{{"Sh0rt!"}, {"tiny"}};
    }

    @Test(dataProvider = "shortPasswords")
    void validate_withTooShortPassword_shouldThrow(String password) {
        assertThatThrownBy(() -> validator.validate(password))
            .isInstanceOf(PasswordTooWeakException.class)
            .hasMessageContaining("at least 8");
    }

    @Test
    void validate_withNoUppercase_shouldThrow() {
        assertThatThrownBy(() -> validator.validate("lowercase1!"))
            .isInstanceOf(PasswordTooWeakException.class)
            .hasMessageContaining("uppercase");
    }

    @Test
    void validate_withNoNumber_shouldThrow() {
        assertThatThrownBy(() -> validator.validate("NoNumber!"))
            .isInstanceOf(PasswordTooWeakException.class)
            .hasMessageContaining("number");
    }

    @Test
    void validate_withNoSpecialChar_shouldThrow() {
        assertThatThrownBy(() -> validator.validate("NoSpecial1"))
            .isInstanceOf(PasswordTooWeakException.class)
            .hasMessageContaining("special");
    }
}
