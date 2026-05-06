package com.whatsnext.authapi.e2e.config;

public final class E2ETestConfig {

    public static final String API_BASE_URL =
            envOrDefault("API_BASE_URL", "http://localhost:8080");

    public static final String JDBC_URL =
            envOrDefault("E2E_JDBC_URL",
                    "jdbc:h2:./target/test-db/testdb;AUTO_SERVER=TRUE;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");

    public static final String DB_USER = envOrDefault("E2E_DB_USER", "sa");
    public static final String DB_PASS = envOrDefault("E2E_DB_PASS", "");
    public static final String USER_PASS = envOrDefault("E2E_USER_PASS", "Test@1234!");

    private E2ETestConfig() {}

    private static String envOrDefault(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
