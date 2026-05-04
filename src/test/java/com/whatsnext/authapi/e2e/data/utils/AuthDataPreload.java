package com.whatsnext.authapi.e2e.data.utils;

import com.whatsnext.authapi.dto.request.LoginRequest;
import com.whatsnext.authapi.dto.response.AuthResponse;
import com.whatsnext.authapi.e2e.data.factory.RequestSpecFactory;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;

import static io.restassured.RestAssured.given;

public class AuthDataPreload {

    public static AuthResponse login(UserCredentialRecord user) {
        return given(RequestSpecFactory.unauthenticatedSpec())
                .body(new LoginRequest(user.email(), user.password()))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().as(AuthResponse.class);
    }

    public static String accessToken(UserCredentialRecord user) {
        return login(user).accessToken();
    }

    public static String refreshToken(UserCredentialRecord user) {
        return login(user).refreshToken();
    }
}