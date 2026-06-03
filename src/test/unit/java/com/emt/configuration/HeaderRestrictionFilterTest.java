package com.emt.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HeaderRestrictionFilterTest {

  private static final String RESTRICTED_HEADER = "X-Blocked-Client";
  private static final String RESTRICTED_VALUE = "legacy-importer";
  private static final String REQUEST_METHOD = "GET";
  private static final String REQUEST_PATH = "/players";

  @Test
  void doFilterInternal_withConfiguredHeaderValuePair_shouldRejectRequest() throws Exception {
    HeaderRestrictionFilter filter = new HeaderRestrictionFilter(restrictionsEnabled());
    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = (request1, response1) -> {};
    request.addHeader(RESTRICTED_HEADER, RESTRICTED_VALUE);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getErrorMessage()).isEqualTo("Request rejected by header restriction policy");
  }

  @Test
  void doFilterInternal_withDifferentHeaderValue_shouldContinueRequest() throws Exception {
    HeaderRestrictionFilter filter = new HeaderRestrictionFilter(restrictionsEnabled());
    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();
    TrackingFilterChain filterChain = new TrackingFilterChain();
    request.addHeader(RESTRICTED_HEADER, "trusted-client");

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(filterChain.invoked).isTrue();
  }

  @Test
  void doFilterInternal_whenRestrictionsDisabled_shouldContinueRequest() throws Exception {
    HeaderRestrictionProperties properties = restrictionsEnabled();
    properties.setEnabled(false);
    HeaderRestrictionFilter filter = new HeaderRestrictionFilter(properties);
    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();
    TrackingFilterChain filterChain = new TrackingFilterChain();
    request.addHeader(RESTRICTED_HEADER, RESTRICTED_VALUE);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(filterChain.invoked).isTrue();
  }

  @Test
  void doFilterInternal_withRepeatedHeaderValues_shouldRejectWhenAnyValueMatches()
      throws Exception {
    HeaderRestrictionFilter filter = new HeaderRestrictionFilter(restrictionsEnabled());
    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = (request1, response1) -> {};
    request.addHeader(RESTRICTED_HEADER, "trusted-client");
    request.addHeader(RESTRICTED_HEADER, RESTRICTED_VALUE);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void doFilterInternal_withoutMatchingRule_shouldDelegateToFilterChain() throws Exception {
    HeaderRestrictionFilter filter = new HeaderRestrictionFilter(restrictionsEnabled());
    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = org.mockito.Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_withRestrictedRequest_shouldNotDelegateToFilterChain() throws Exception {
    HeaderRestrictionFilter filter = new HeaderRestrictionFilter(restrictionsEnabled());
    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = org.mockito.Mockito.mock(FilterChain.class);
    request.addHeader(RESTRICTED_HEADER, RESTRICTED_VALUE);

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(request, response);
  }

  private HeaderRestrictionProperties restrictionsEnabled() {
    HeaderRestrictionProperties properties = new HeaderRestrictionProperties();
    HeaderRestrictionProperties.Rule rule = new HeaderRestrictionProperties.Rule();
    rule.setHeaderName(RESTRICTED_HEADER);
    rule.setHeaderValue(RESTRICTED_VALUE);
    properties.setRules(List.of(rule));
    return properties;
  }

  private MockHttpServletRequest request() {
    return new MockHttpServletRequest(REQUEST_METHOD, REQUEST_PATH);
  }

  private static class TrackingFilterChain implements FilterChain {

    private boolean invoked;

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
      invoked = true;
    }
  }
}
