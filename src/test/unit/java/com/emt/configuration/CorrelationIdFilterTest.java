package com.emt.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private static final String REQUEST_METHOD = "GET";
  private static final String REQUEST_PATH = "/players";

  @Test
  void doFilterInternal_withoutCorrelationHeader_ShouldGenerateCorrelationId() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest(REQUEST_METHOD, REQUEST_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotBlank();
  }

  @Test
  void doFilterInternal_withBlankCorrelationHeader_ShouldGenerateCorrelationId() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest(REQUEST_METHOD, REQUEST_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
        .isNotBlank()
        .isNotEqualTo("   ");
  }

  @Test
  void doFilterInternal_withCorrelationHeader_ShouldReuseCorrelationId() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest(REQUEST_METHOD, REQUEST_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123");

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
        .isEqualTo("request-123");
  }

  @Test
  void doFilterInternal_afterRequest_ShouldClearCorrelationIdFromMdc() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest(REQUEST_METHOD, REQUEST_PATH);
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-456");

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
  }
}
