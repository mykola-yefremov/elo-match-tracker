package com.emt.configuration;

import com.emt.metrics.BusinessMetrics;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(HeaderRestrictionProperties.class)
public class RequestFilteringConfiguration {

  @Bean
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
    FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new CorrelationIdFilter());
    registration.setName("correlationIdFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }

  @Bean
  public FilterRegistrationBean<HeaderRestrictionFilter> headerRestrictionFilter(
      HeaderRestrictionProperties properties, BusinessMetrics businessMetrics) {
    FilterRegistrationBean<HeaderRestrictionFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new HeaderRestrictionFilter(properties, businessMetrics));
    registration.setName("headerRestrictionFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
