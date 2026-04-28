package com.whatsnext.authapi.dto.response;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ErrorResponse(List<ErrorItem> errors, Meta meta) {

    public record ErrorItem(String code, String title, String detail) {}

    public record Meta(String requestDateTime) {
        private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

        static Meta now() {
            return new Meta(DT_FORMAT.format(Instant.now()));
        }
    }

    public static ErrorResponse of(String code, String title, String detail) {
        return new ErrorResponse(List.of(new ErrorItem(code, title, detail)), Meta.now());
    }

    public static ErrorResponse ofErrors(List<ErrorItem> items) {
        return new ErrorResponse(items, Meta.now());
    }
}
