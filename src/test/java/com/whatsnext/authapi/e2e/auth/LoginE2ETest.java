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
import org.testng.asserts.SoftAssert;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

@Epic("Auth API")
@Feature("Login")
public class LoginE2ETest {

    private static final String INVALID_CREDENTIALS_DETAIL = "Invalid credentials";
    private static final String UNAUTHORIZED_TITLE          = "Unauthorized";
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
        authClient.login(validUserCredential.email(), validUserCredential.password()).assertThat()
                .statusCode(SC_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/auth-response-schema.json"));
    }

    @Test
    @Story("Email inexistente")
    public void loginWithNonexistentEmail() {
        authClient.login("ghost@test.com", "Test@1234!" )
                .statusCode(SC_UNAUTHORIZED)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"))
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNAUTHORIZED)),
                        "errors[0].title", equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL)
                );
    }

    @Test
    @Story("Senha errada")
    public void loginWithWrongPassword() {
        authClient.login( validUserCredential.email(), "WrongPass@99!")
                .statusCode(SC_UNAUTHORIZED)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"))
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNAUTHORIZED)),
                        "errors[0].title", equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL)
                );
    }

    @Test(dataProvider = "invalidLoginFields", dataProviderClass = LoginDataProvider.class)
    @Story("Validação de campos inválidos")
    public void loginWithInvalidFields(String email, String password, String expectedDetail) {
        authClient.login(email,password)
                .statusCode(SC_UNPROCESSABLE_ENTITY)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"))
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNPROCESSABLE_ENTITY)),
                        "errors[0].title", equalTo("Validation Error"),
                        "errors[0].detail", equalTo(expectedDetail)
                );
    }
}