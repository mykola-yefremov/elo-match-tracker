package com.emt.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class HeaderRestrictionFilter extends OncePerRequestFilter {

  private static final String REJECTION_MESSAGE = "Request rejected by header restriction policy";

  private final HeaderRestrictionProperties properties;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (rejectRestrictedRequest(request, response)) {
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean rejectRestrictedRequest(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var matchingRule = properties.findMatchingRule(request);
    if (matchingRule.isEmpty()) {
      return false;
    }

    log.warn(
        "request_rejected_by_header_restriction uri={} method={} header={}",
        request.getRequestURI(),
        request.getMethod(),
        matchingRule.get().getHeaderName());
    response.sendError(HttpStatus.FORBIDDEN.value(), REJECTION_MESSAGE);
    return true;
  }
}
