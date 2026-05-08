package com.whatsnext.authapi.unit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsnext.authapi.exception.InvalidTokenException;
import com.whatsnext.authapi.exception.TokenExpiredException;
import com.whatsnext.authapi.filter.JwtAuthenticationFilter;
import com.whatsnext.authapi.service.JwtService;
import com.whatsnext.authapi.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService,
            tokenBlacklistService, new ObjectMapper());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterMethod
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withoutAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, tokenBlacklistService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withWrongPrefix_continuesChainWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic abc123");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, tokenBlacklistService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withValidBearerToken_setsAuthenticationAndContinuesChain() throws Exception {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = new User("user@example.com", "pwd",
            Collections.emptyList());
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractSubject(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
            .isEqualTo(userDetails);
    }

    @Test
    void doFilter_withBlacklistedToken_writes401AndStopsChain() throws Exception {
        String token = "revoked.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token has been revoked");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withExpiredToken_writes401AndStopsChain() throws Exception {
        String token = "expired.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractSubject(token)).thenThrow(new TokenExpiredException());

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token has expired");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withInvalidToken_writes401AndStopsChain() throws Exception {
        String token = "garbage";
        request.addHeader("Authorization", "Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractSubject(token)).thenThrow(new InvalidTokenException("bad"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_whenTokenSubjectMismatch_doesNotSetAuthenticationButContinuesChain() throws Exception {
        String token = "subject.mismatch.token";
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = new User("user@example.com", "pwd",
            Collections.emptyList());
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractSubject(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
