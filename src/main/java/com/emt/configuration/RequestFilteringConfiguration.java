package com.emt.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(HeaderRestrictionProperties.class)
public class RequestFilteringConfiguration {

  @Bean
  public FilterRegistrationBean<HeaderRestrictionFilter> headerRestrictionFilter(
      HeaderRestrictionProperties properties) {
    FilterRegistrationBean<HeaderRestrictionFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new HeaderRestrictionFilter(properties));
    registration.setName("headerRestrictionFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
