package ru.hh.nab.sentry.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.servlet.ServletSystemFilterPriorities;
import ru.hh.nab.common.spring.boot.web.servlet.SystemFilterRegistrationBean;
import ru.hh.nab.sentry.SentryFilter;

@AutoConfiguration
public class NabSentryAutoConfiguration {

  @Configuration
  @ConditionalOnWebApplication(type = Type.SERVLET)
  public static class SentryFilterConfiguration {
    @Bean
    public SystemFilterRegistrationBean<SentryFilter> sentryFilter() {
      SystemFilterRegistrationBean<SentryFilter> registration = new SystemFilterRegistrationBean<>(new SentryFilter());
      registration.setOrder(ServletSystemFilterPriorities.SYSTEM_OBSERVABILITY);
      return registration;
    }
  }
}
