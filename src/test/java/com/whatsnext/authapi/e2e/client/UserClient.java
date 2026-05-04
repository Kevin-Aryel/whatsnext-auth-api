package com.whatsnext.authapi.e2e.client;

import com.whatsnext.authapi.e2e.data.factory.RequestSpecFactory;
import io.restassured.response.ValidatableResponse;

import static io.restassured.RestAssured.given;

public class UserClient {

    private static final String USER = "/api/v1/user";

    public ValidatableResponse getProfile(String accessToken) {
        return given(RequestSpecFactory.authenticatedSpec(accessToken))
                .get(USER)
                .then().log().all();
    }

    public ValidatableResponse getProfileWithoutToken() {
        return given(RequestSpecFactory.unauthenticatedSpec())
                .get(USER)
                .then().log().all();
    }
}