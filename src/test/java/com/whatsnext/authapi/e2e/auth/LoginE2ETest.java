package com.whatsnext.authapi.e2e.auth;

import com.whatsnext.authapi.e2e.client.AuthClient;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import com.whatsnext.authapi.e2e.data.factory.UserFactory;
import com.whatsnext.authapi.e2e.data.provider.LoginDataProvider;
import com.whatsnext.authapi.e2e.data.utils.UserDataPreload;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

@Epic("Auth API")
@Feature("Login")
public class LoginE2ETest {

    private static final String INVALID_CREDENTIALS_DETAIL = "Invalid credentials";

    private AuthClient authClient;
    private UserCredentialRecord validUserCredential;

    @BeforeClass
    public void setup() throws SQLException {
        authClient = new AuthClient();
        validUserCredential = UserFactory.validUserCredentials();
        UserDataPreload.insertUser(validUserCredential);
    }

    @Test
    @Story("Login com credenciais válidas")
    public void loginWithValidCredentials() {
        authClient.login(validUserCredential.email(), validUserCredential.password() )
                .statusCode(SC_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/auth-response-schema.json"));
    }

    @Test
    @Story("Email inexistente")
    public void loginWithNonexistentEmail() {
        authClient.login("ghost@test.com", "Test@1234!" )
                .statusCode(SC_UNAUTHORIZED)
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNAUTHORIZED)),
                        "errors[0].title", equalTo(INVALID_CREDENTIALS_DETAIL),
                        "errors[0].detail", equalTo(""),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }

    @Test
    @Story("Senha errada")
    public void loginWithWrongPassword() {
        authClient.login( validUserCredential.email(), "WrongPass@99!")
                .statusCode(SC_UNAUTHORIZED)
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNAUTHORIZED)),
                        "errors[0].title", equalTo(INVALID_CREDENTIALS_DETAIL),
                        "errors[0].detail", equalTo(""),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }

    @Test(dataProvider = "invalidLoginFields", dataProviderClass = LoginDataProvider.class)
    @Story("Validação de campos inválidos")
    public void loginWithInvalidFields(String email, String password, String expectedDetail) {
        authClient.login(email,password)
                .statusCode(SC_UNPROCESSABLE_ENTITY)
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNPROCESSABLE_ENTITY)),
                        "errors[0].title", equalTo("Validation Error"),
                        "errors[0].detail", equalTo(""),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }
}