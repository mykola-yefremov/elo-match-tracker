package com.emt.configuration;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

  private static final String ADMIN = "ADMIN";
  private static final String API_PATTERN = "/api/**";

  private final AppSecurityProperties securityProperties;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers(API_PATTERN))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/styles/**", "/login", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers(POST, "/players/register", "/matches/report", "/matches/cancel")
                    .hasRole(ADMIN)
                    .requestMatchers(POST, "/tournaments/**")
                    .hasRole(ADMIN)
                    .requestMatchers(POST, "/api/v1/players", "/api/v1/matches", "/api/v1/tournaments/**")
                    .hasRole(ADMIN)
                    .requestMatchers(DELETE, "/api/v1/matches/**")
                    .hasRole(ADMIN)
                    .anyRequest()
                    .permitAll())
        .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/players", true).permitAll())
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(
            handling ->
                handling
                    .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher(API_PATTERN))
                    .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new NegatedRequestMatcher(new AntPathRequestMatcher(API_PATTERN))))
        .build();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    AppSecurityProperties.User admin = securityProperties.getAdmin();
    AppSecurityProperties.User user = securityProperties.getUser();

    return new InMemoryUserDetailsManager(
        User.withUsername(admin.getUsername())
            .password(passwordEncoder.encode(admin.getPassword()))
            .roles(ADMIN)
            .build(),
        User.withUsername(user.getUsername())
            .password(passwordEncoder.encode(user.getPassword()))
            .roles("USER")
            .build());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
