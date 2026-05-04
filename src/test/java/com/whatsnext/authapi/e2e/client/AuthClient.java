package com.whatsnext.authapi.e2e.client;

import com.whatsnext.authapi.dto.request.LoginRequest;
import com.whatsnext.authapi.dto.request.RefreshRequest;
import com.whatsnext.authapi.dto.request.RegisterRequest;
import com.whatsnext.authapi.e2e.data.factory.RequestSpecFactory;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

import static io.restassured.RestAssured.given;

public class AuthClient {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN    = "/api/v1/auth/login";
    private static final String REFRESH  = "/api/v1/auth/refresh";
    private static final String LOGOUT   = "/api/v1/auth/logout";

    public ValidatableResponse register(UserCredentialRecord user) {
        return given(RequestSpecFactory.unauthenticatedSpec())
                .body(new RegisterRequest(user.name(), user.email(), user.password()))
                .post(REGISTER)
                .then().log().all();
    }

    public ValidatableResponse login(String email, String password) {
        return given(RequestSpecFactory.unauthenticatedSpec())
                .body(new LoginRequest(email, password))
                .post(LOGIN)
                .then().log().all();
    }
    public ValidatableResponse refresh(String refreshToken) {
        return given(RequestSpecFactory.unauthenticatedSpec())
                .body(new RefreshRequest(refreshToken))
                .post(REFRESH)
                .then().log().all();
    }

    public ValidatableResponse logout(String accessToken) {
        return given(RequestSpecFactory.authenticatedSpec(accessToken))
                .post(LOGOUT)
                .then().log().all();
    }

    public ValidatableResponse logoutWithoutToken() {
        return given(RequestSpecFactory.unauthenticatedSpec())
                .post(LOGOUT)
                .then().log().all();
    }
}