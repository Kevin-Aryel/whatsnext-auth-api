package com.whatsnext.authapi.e2e.data.utils;

import com.whatsnext.authapi.e2e.config.E2ETestConfig;
import com.whatsnext.authapi.e2e.data.factory.UserCredentialRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;
import java.util.UUID;

public class UserDataPreload {

    private static final String PASSWORD_HASH =
            new BCryptPasswordEncoder(12).encode(E2ETestConfig.USER_PASS);

    public static void insertUser(UserCredentialRecord user) throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                E2ETestConfig.JDBC_URL, E2ETestConfig.DB_USER, E2ETestConfig.DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (id, name, email, password_hash, role, created_at, updated_at)" +
                             " VALUES (?, ?, ?, ?, 'USER', NOW(), NOW())")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, user.name());
            ps.setString(3, user.email());
            ps.setString(4, PASSWORD_HASH);
            ps.executeUpdate();
        }
    }
}