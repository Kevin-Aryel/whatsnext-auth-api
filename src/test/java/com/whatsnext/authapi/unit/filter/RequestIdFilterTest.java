package com.whatsnext.authapi.unit.filter;

import com.whatsnext.authapi.filter.RequestIdFilter;
import jakarta.servlet.FilterChain;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RequestIdFilterTest {

    private RequestIdFilter filter;
    private FilterChain chain;

    @BeforeMethod
    void setUp() {
        filter = new RequestIdFilter();
        chain = mock(FilterChain.class);
        MDC.clear();
    }

    @AfterMethod
    void tearDown() {
        MDC.clear();
    }

    @Test
    void noIncomingHeader_generatesUuidInResponseAndMdc() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        String[] mdcValueDuringChain = new String[1];

        doAnswer(inv -> {
            mdcValueDuringChain[0] = MDC.get(RequestIdFilter.MDC_KEY);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(req, res, chain);

        String header = res.getHeader(RequestIdFilter.HEADER);
        assertThat(header).isNotBlank();
        assertThat(header).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(mdcValueDuringChain[0]).isEqualTo(header);
    }

    @Test
    void mdcIsRemovedAfterFilterReturns() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void incomingRequestIdHeader_isPropagated() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(RequestIdFilter.HEADER, "trace-abc-123");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader(RequestIdFilter.HEADER)).isEqualTo("trace-abc-123");
    }

    @Test
    void incomingHeaderTooLong_isTruncated() throws Exception {
        String oversize = "x".repeat(200);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(RequestIdFilter.HEADER, oversize);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader(RequestIdFilter.HEADER)).hasSize(64);
    }

    @Test
    void blankIncomingHeader_isReplacedWithGeneratedId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(RequestIdFilter.HEADER, "   ");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        String header = res.getHeader(RequestIdFilter.HEADER);
        assertThat(header).isNotBlank();
        assertThat(header).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void eachRequestGetsIndependentId() throws Exception {
        MockHttpServletResponse a = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), a, chain);

        MockHttpServletResponse b = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), b, chain);

        assertThat(a.getHeader(RequestIdFilter.HEADER))
                .isNotEqualTo(b.getHeader(RequestIdFilter.HEADER));
    }
}
