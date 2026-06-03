package com.emt.audit;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

  @NotBlank
  private String actorHeader = "X-Actor";

  @NotBlank
  private String fallbackActor = "system";
}
