package com.emt.configuration;

import jakarta.validation.Valid;
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
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

  @Valid private User admin = new User("admin", "admin-pass");

  @Valid private User user = new User("user", "user-pass");

  @Getter
  @Setter
  public static class User {

    @NotBlank private String username;
    @NotBlank private String password;

    public User() {

    }

    public User(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }
}
