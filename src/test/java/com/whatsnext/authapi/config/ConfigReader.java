package com.whatsnext.authapi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties props = load();

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config-local.properties")) {
            if (in == null) throw new RuntimeException("config-local.properties não encontrado");
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao carregar config-local.properties", e);
        }
        return p;
    }

    public static String get(String key) {
        String envKey = key.replace(".", "_").toUpperCase();
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : props.getProperty(key);
    }
}