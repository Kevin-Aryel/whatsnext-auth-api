package com.whatsnext.authapi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties props = load();

    private static Properties load() {
        Properties p = new Properties();
        loadFile(p, "config.properties");
        loadFile(p, "config-local.properties");
        return p;
    }

    private static void loadFile(Properties p, String filename) {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(filename)) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            // ignore
        }
    }

    public static String get(String key) {
        String envKey = key.replace(".", "_").toUpperCase();
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : props.getProperty(key);
    }
}
