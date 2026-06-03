package com.emt.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuditActorProvider {

  private final AuditProperties auditProperties;

  public String currentActor() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return actorFrom(attributes.getRequest());
    }
    return auditProperties.getFallbackActor();
  }

  private String actorFrom(HttpServletRequest request) {
    String actor = request.getHeader(auditProperties.getActorHeader());
    if (StringUtils.hasText(actor)) {
      return actor;
    }
    return auditProperties.getFallbackActor();
  }
}
