package com.whatsnext.authapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitConfig {

    private EndpointConfig login = new EndpointConfig(5, 60);
    private EndpointConfig register = new EndpointConfig(3, 60);
    private EndpointConfig refresh = new EndpointConfig(10, 60);

    @Getter
    @Setter
    public static class EndpointConfig {
        private int capacity;
        private int refillSeconds;

        public EndpointConfig(int capacity, int refillSeconds) {
            this.capacity = capacity;
            this.refillSeconds = refillSeconds;
        }
    }
}
