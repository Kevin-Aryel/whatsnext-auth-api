package com.whatsnext.authapi.e2e.auth;

import com.whatsnext.authapi.e2e.client.AuthClient;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import com.whatsnext.authapi.e2e.data.factory.UserFactory;
import com.whatsnext.authapi.e2e.data.provider.RegisterDataProvider;
import com.whatsnext.authapi.e2e.data.utils.UserDataPreload;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

@Epic("Auth API")
@Feature("Register")
public class RegisterE2ETest {

    private AuthClient authClient;

    @BeforeClass
    public void setup() {
        authClient = new AuthClient();
    }

    @Test
    @Story("Cadastro com dados válidos")
    public void registerWithValidData() {
        authClient.register(UserFactory.validUserCredentials())
                .statusCode(SC_CREATED);
    }

    @Test
    @Story("Email duplicado")
    public void registerWithDuplicateEmail() throws SQLException {
        UserCredentialRecord user = UserFactory.validUserCredentials();
        UserDataPreload.insertUser(user);
        authClient.register(user)
                .statusCode(SC_CONFLICT)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"))
                .body(
                        "errors[0].code",   equalTo(String.valueOf(SC_CONFLICT)),
                        "errors[0].title",  equalTo("Email Already Exists"),
                        "errors[0].detail", containsString("Email already registered")
                );
    }

    @Test(dataProvider = "invalidFields", dataProviderClass = RegisterDataProvider.class)
    @Story("Validação de campos inválidos")
    public void registerWithInvalidFields(
            String scenario, UserCredentialRecord user, String expectedTitle, String expectedDetail) {
        authClient.register(user)
                .statusCode(SC_UNPROCESSABLE_ENTITY)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"))
                .body(
                        "errors[0].code",equalTo(String.valueOf(SC_UNPROCESSABLE_ENTITY)),
                        "errors[0].title",  equalTo(expectedTitle),
                        "errors[0].detail", containsString(expectedDetail)
                );
    }

    @Test(dataProvider = "invalidPasswords", dataProviderClass = RegisterDataProvider.class)
    @Story("Validação de senha fraca")
    public void registerWithInvalidPassword(
            String scenario, UserCredentialRecord user, String expectedTitle, String expectedDetail) {
        authClient.register(user)
                .statusCode(SC_UNPROCESSABLE_ENTITY)
                .body(
                        "errors[0].code", equalTo(String.valueOf(SC_UNPROCESSABLE_ENTITY)),
                        "errors[0].title",  equalTo(expectedTitle),
                        "errors[0].detail", containsString(expectedDetail)
                );
    }
}




