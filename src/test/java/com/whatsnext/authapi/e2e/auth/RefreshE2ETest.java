package com.whatsnext.authapi.e2e.auth;

import com.whatsnext.authapi.e2e.client.AuthClient;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import com.whatsnext.authapi.e2e.data.factory.UserFactory;
import com.whatsnext.authapi.e2e.data.provider.RefreshDataProvider;
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
@Feature("Refresh Token")
public class RefreshE2ETest {

    private static final String INVALID_CREDENTIALS_DETAIL = "Invalid credentials";
    private static final String UNAUTHORIZED_CODE  = "401";
    private static final String UNAUTHORIZED_TITLE = "UNAUTHORIZED";

    private AuthClient authClient;
    private UserCredentialRecord existingUser;

    @BeforeClass
    public void setup() throws SQLException {
        authClient = new AuthClient();
        existingUser = UserFactory.validUserCredentials();
        UserDataPreload.insertUser(existingUser);
    }

    @Test
    @Story("Refresh token válido")
    public void refreshWithValidToken(){
        String refreshToken = AuthDataPreload.refreshToken(existingUser);

        authClient.refresh(refreshToken)
                .statusCode(SC_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/auth-response-schema.json"));
    }

    @Test
    @Story("Token já utilizado após rotação")
    public void refreshWithAlreadyUsedToken() {
        String refreshToken = AuthDataPreload.refreshToken(existingUser);

        authClient.refresh(refreshToken)
                .statusCode(SC_OK);

        authClient.refresh(refreshToken)
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL),
                        JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"));
    }

    @Test
    @Story("Token antigo após rotação")
    public void refreshWithOldTokenAfterRotation() {
        String oldRefreshToken = AuthDataPreload.refreshToken(existingUser);
        authClient.refresh(oldRefreshToken)
                .statusCode(SC_OK);

        authClient.refresh(oldRefreshToken)
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL),
                        JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"));
    }

    @Test(dataProvider = "invalidRefreshTokens", dataProviderClass = RefreshDataProvider.class)
    @Story("Token inválido")
    public void refresh_invalidToken_returns401(String token, String expectedDetail) {
        authClient.refresh(token)
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(expectedDetail),
                        JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json"));
    }
}