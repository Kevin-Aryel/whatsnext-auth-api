package com.whatsnext.authapi.e2e.user;

import com.whatsnext.authapi.e2e.client.AuthClient;
import com.whatsnext.authapi.e2e.client.UserClient;
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
@Feature("User Profile")
public class UserE2ETest {

    private static final String UNAUTHORIZED_CODE           = String.valueOf(SC_UNAUTHORIZED);
    private static final String UNAUTHORIZED_TITLE          = "Unauthorized";
    private static final String INVALID_CREDENTIALS_DETAIL  = "Invalid credentials";

    private UserClient userClient;
    private AuthClient authClient;
    private UserCredentialRecord existingUser;

    @BeforeClass
    public void setup() throws SQLException {
        userClient   = new UserClient();
        authClient   = new AuthClient();
        existingUser = UserFactory.validUserCredentials();
        UserDataPreload.insertUser(existingUser);
    }

    @Test
    @Story("Perfil com Bearer válido")
    public void getProfileWithValidBearer() {
        String accessToken = AuthDataPreload.accessToken(existingUser);

        userClient.getProfile(accessToken)
                .statusCode(SC_OK)
                .body("email", equalTo(existingUser.email()),
                        "role",  equalTo("USER"),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-profile-schema.json")
                );
    }

    @Test
    @Story("Perfil sem token")
    public void getProfileWithoutToken() {
        userClient.getProfileWithoutToken()
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }

    @Test
    @Story("Perfil com token inválido")
    public void getProfileWithInvalidToken() {
        userClient.getProfile("token.invalido.assinado")
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }

    @Test
    @Story("Perfil com token blacklisted após logout")
    public void getProfileWithBlacklistedToken() {
        String accessToken = AuthDataPreload.accessToken(existingUser);

        authClient.logout(accessToken)
                .statusCode(SC_NO_CONTENT);

        userClient.getProfile(accessToken)
                .statusCode(SC_UNAUTHORIZED)
                .body("errors[0].code",   equalTo(UNAUTHORIZED_CODE),
                        "errors[0].title",  equalTo(UNAUTHORIZED_TITLE),
                        "errors[0].detail", equalTo(INVALID_CREDENTIALS_DETAIL),
                        "", JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/error-response-schema.json")
                );
    }
}