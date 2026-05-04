package com.whatsnext.authapi.e2e.data.factory;

import com.whatsnext.authapi.config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class RequestSpecFactory {

    private static final String BASE_URL = ConfigReader.get("api.base.url");

    public static RequestSpecification unauthenticatedSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }

    public static RequestSpecification authenticatedSpec(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(unauthenticatedSpec())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}