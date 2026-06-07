package com.emt.audit;

import jakarta.servlet.http.HttpServletRequest;
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
    String securityActor = actorFromSecurityContext();
    if (StringUtils.hasText(securityActor)) {
      return securityActor;
    }
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return actorFrom(attributes.getRequest());
    }
    return auditProperties.getFallbackActor();
  }

  private String actorFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }
    if (!authentication.isAuthenticated()) {
      return null;
    }
    return authentication.getName();
  }

  private String actorFrom(HttpServletRequest request) {
    String actor = request.getHeader(auditProperties.getActorHeader());
    if (StringUtils.hasText(actor)) {
      return actor;
    }
    return auditProperties.getFallbackActor();
  }
}
