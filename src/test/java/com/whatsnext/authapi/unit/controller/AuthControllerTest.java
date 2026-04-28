package com.whatsnext.authapi.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsnext.authapi.dto.request.LoginRequest;
import com.whatsnext.authapi.dto.request.RefreshRequest;
import com.whatsnext.authapi.dto.request.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String BASE = "/api/v1/auth";

    @Test
    void register_withValidPayload_returns201WithTokens() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Alice", "alice@example.com", "Secure@123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest("Bob", "bob@example.com", "Secure@123");
        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON).content(body));
        mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidEmail_returns422() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("C", "not-an-email", "Secure@123"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_withShortPassword_returns422() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Dan", "dan@example.com", "weak"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_withBlankName_returns422() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("", "e@example.com", "Secure@123"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void login_withValidCredentials_returns200WithTokens() throws Exception {
        register("login@example.com", "Secure@123");

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("login@example.com", "Secure@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_withUnknownEmail_returns401WithGenericMessage() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("ghost@example.com", "Secure@123"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Invalid credentials");
    }

    @Test
    void login_withWrongPassword_returns401WithSameGenericMessage() throws Exception {
        register("wrongpass@example.com", "Secure@123");

        MvcResult result = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("wrongpass@example.com", "Wrong@999"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Invalid credentials");
    }

    @Test
    void refresh_withValidToken_returns200WithNewTokens() throws Exception {
        String refreshToken = registerAndGetRefreshToken("refresh@example.com");

        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_withUnknownToken_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("unknown-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withAlreadyUsedToken_returns401() throws Exception {
        String refreshToken = registerAndGetRefreshToken("usedtoken@example.com");

        mockMvc.perform(post(BASE + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))));

        mockMvc.perform(post(BASE + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withValidTokens_returns204() throws Exception {
        MvcResult loginResult = register("logout@example.com", "Secure@123");
        String body = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(body).get("refreshToken").asText();

        mockMvc.perform(post(BASE + "/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isNoContent());
    }

    private MvcResult register(String email, String password) throws Exception {
        return mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("User", email, password))))
                .andReturn();
    }

    private String registerAndGetRefreshToken(String email) throws Exception {
        MvcResult result = register(email, "Secure@123");
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("refreshToken").asText();
    }
}
