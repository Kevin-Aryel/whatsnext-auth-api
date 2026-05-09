package com.whatsnext.authapi.unit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.testng.annotations.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityHeadersTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoint_shouldHaveXContentTypeOptions() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void publicEndpoint_shouldHaveXFrameOptionsDeny() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void publicEndpoint_shouldHaveStrictReferrerPolicy() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void publicEndpoint_shouldHaveHstsHeader() throws Exception {
        // Spring sends HSTS only over secure channels. MockMvc requests are non-secure
        // by default, so we issue the request as secure to validate the value.
        mockMvc.perform(get("/actuator/health").secure(true))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000 ; includeSubDomains"));
    }
}
