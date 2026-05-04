package com.whatsnext.authapi.e2e.auth;

import com.whatsnext.authapi.e2e.client.AuthClient;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import com.whatsnext.authapi.e2e.data.factory.UserFactory;
import com.whatsnext.authapi.e2e.data.utils.AuthDataPreload;
import com.whatsnext.authapi.e2e.data.utils.UserDataPreload;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

@Epic("Auth API")
@Feature("Logout")
public class LogoutE2ETest {

    private static final String UNAUTHORIZED_CODE           = "401";
    private static final String UNAUTHORIZED_TITLE          = "UNAUTHORIZED";
    private static final String INVALID_CREDENTIALS_DETAIL  = "Invalid credentials";

    private AuthClient authClient;
    private UserCredentialRecord existingUser;

    @BeforeClass
    public void setup() throws SQLException {
        authClient   = new AuthClient();
        existingUser = UserFactory.validUserCredentials();
        UserDataPreload.insertUser(existingUser);
    }

    @Test
    @Story("Logout com Bearer válido")
    public void logoutWithValidBearer() {
        String accessToken = AuthDataPreload.accessToken(existingUser);
        authClient.logout(accessToken)
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    @Story("Logout sem token")
    public void logoutWithoutToken() {
        authClient.logoutWithoutToken()
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL),
                        JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }
}