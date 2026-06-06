package com.emt.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "request-filter.header-restrictions")
public class HeaderRestrictionProperties {

  @Valid private List<Rule> rules = new ArrayList<>();

  private boolean enabled = true;

  public Optional<Rule> findMatchingRule(HttpServletRequest request) {
    if (!enabled || rules.isEmpty()) {
      return Optional.empty();
    }

    return rules.stream().filter(rule -> rule.matches(request)).findFirst();
  }

  @Getter
  @Setter
  public static class Rule {

    @NotBlank private String headerName;

    @NotBlank private String headerValue;

    boolean matches(HttpServletRequest request) {
      Enumeration<String> headerValues = request.getHeaders(headerName);
      while (headerValues.hasMoreElements()) {
        if (headerValue.equals(headerValues.nextElement())) {
          return true;
        }
      }
      return false;
    }
  }
}
