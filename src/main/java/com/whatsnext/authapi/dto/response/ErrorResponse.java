package com.whatsnext.authapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ErrorResponse(List<ErrorItem> errors, Meta meta) {

    public record ErrorItem(String code, String title, String detail) {}

    public record Meta(String requestDateTime,
                       @JsonInclude(JsonInclude.Include.NON_NULL) String requestId) {

        // Mirrors RequestIdFilter.MDC_KEY. Kept as a literal here to avoid pulling
        // the filter package into the DTO layer.
        private static final String REQUEST_ID_MDC_KEY = "requestId";

        private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

        static Meta now() {
            return new Meta(DT_FORMAT.format(Instant.now()), MDC.get(REQUEST_ID_MDC_KEY));
        }
    }

    public static ErrorResponse of(String code, String title, String detail) {
        return new ErrorResponse(List.of(new ErrorItem(code, title, detail)), Meta.now());
    }

    public static ErrorResponse ofErrors(List<ErrorItem> items) {
        return new ErrorResponse(items, Meta.now());
    }
}
