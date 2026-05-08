package com.whatsnext.authapi.unit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsnext.authapi.config.RateLimitConfig;
import com.whatsnext.authapi.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeMethod
    void setUp() {
        RateLimitConfig config = new RateLimitConfig();
        config.setLogin(new RateLimitConfig.EndpointConfig(1, 60));
        config.setRegister(new RateLimitConfig.EndpointConfig(1, 60));
        config.setRefresh(new RateLimitConfig.EndpointConfig(1, 60));
        filter = new RateLimitFilter(config, new ObjectMapper());
        chain = mock(FilterChain.class);
    }

    @Test
    void unmatchedPath_shouldPassThroughWithoutBucket() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/user");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(429);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void firstLogin_shouldPassThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(429);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void secondLoginFromSameIp_shouldReturn429WithErrorBody() throws Exception {
        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        first.setRemoteAddr("10.0.0.1");
        filter.doFilter(first, new MockHttpServletResponse(), chain);

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        second.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(second, res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("60");
        assertThat(res.getContentAsString())
                .contains("\"code\":\"429\"")
                .contains("\"title\":\"Too Many Requests\"");
        verify(chain, never()).doFilter(second, res);
    }

    @Test
    void refresh_secondRequest_shouldReturn429() throws Exception {
        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        first.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse firstRes = new MockHttpServletResponse();
        filter.doFilter(first, firstRes, chain);
        assertThat(firstRes.getStatus()).isNotEqualTo(429);

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        second.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse secondRes = new MockHttpServletResponse();
        filter.doFilter(second, secondRes, chain);
        assertThat(secondRes.getStatus()).isEqualTo(429);
    }

    @Test
    void differentEndpoints_haveIndependentBuckets() throws Exception {
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        login.setRemoteAddr("10.0.0.3");
        MockHttpServletRequest register = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        register.setRemoteAddr("10.0.0.3");

        filter.doFilter(login, new MockHttpServletResponse(), chain);
        MockHttpServletResponse loginThrottled = new MockHttpServletResponse();
        filter.doFilter(login, loginThrottled, chain);
        assertThat(loginThrottled.getStatus()).isEqualTo(429);

        MockHttpServletResponse registerOk = new MockHttpServletResponse();
        filter.doFilter(register, registerOk, chain);
        assertThat(registerOk.getStatus()).isNotEqualTo(429);
    }

    @Test
    void differentIps_haveIndependentBuckets() throws Exception {
        MockHttpServletRequest a = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        a.setRemoteAddr("10.0.0.10");
        MockHttpServletRequest b = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        b.setRemoteAddr("10.0.0.11");

        filter.doFilter(a, new MockHttpServletResponse(), chain);
        MockHttpServletResponse aThrottled = new MockHttpServletResponse();
        filter.doFilter(a, aThrottled, chain);
        assertThat(aThrottled.getStatus()).isEqualTo(429);

        MockHttpServletResponse bOk = new MockHttpServletResponse();
        filter.doFilter(b, bOk, chain);
        assertThat(bOk.getStatus()).isNotEqualTo(429);
    }

    @Test
    void spoofedXForwardedFor_doesNotEscapeRateLimit() throws Exception {
        // Same physical client (remoteAddr) tries to dodge the bucket by varying X-Forwarded-For.
        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        first.setRemoteAddr("10.0.0.99");
        first.addHeader("X-Forwarded-For", "1.2.3.4");

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        second.setRemoteAddr("10.0.0.99");
        second.addHeader("X-Forwarded-For", "5.6.7.8");

        MockHttpServletResponse firstRes = new MockHttpServletResponse();
        filter.doFilter(first, firstRes, chain);
        assertThat(firstRes.getStatus()).isNotEqualTo(429);

        MockHttpServletResponse secondRes = new MockHttpServletResponse();
        filter.doFilter(second, secondRes, chain);
        assertThat(secondRes.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(second, secondRes);
    }
}
