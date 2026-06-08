package com.emt.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuditActorProvider {

  private final AuditProperties auditProperties;

  public String currentActor() {
    return actorFromSecurityContext()
        .or(this::actorFromRequest)
        .orElse(auditProperties.getFallbackActor());
  }

  private Optional<String> actorFromSecurityContext() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken))
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getName)
        .filter(StringUtils::hasText);
  }

  private Optional<String> actorFromRequest() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(ServletRequestAttributes.class::isInstance)
        .map(ServletRequestAttributes.class::cast)
        .map(ServletRequestAttributes::getRequest)
        .flatMap(this::actorFrom);
  }

  private Optional<String> actorFrom(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader(auditProperties.getActorHeader()))
        .filter(StringUtils::hasText);
  }
}
