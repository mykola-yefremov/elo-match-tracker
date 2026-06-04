package com.emt.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  @Test
  void doFilterInternal_withoutCorrelationHeader_ShouldGenerateCorrelationId() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/players");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotBlank();
  }

  @Test
  void doFilterInternal_withCorrelationHeader_ShouldReuseCorrelationId() throws Exception {
    CorrelationIdFilter filter = new CorrelationIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/players");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123");

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
        .isEqualTo("request-123");
  }
}
