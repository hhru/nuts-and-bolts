package ru.hh.nab.sentry.starter;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import ru.hh.nab.common.spring.boot.env.EnvironmentUtils;
import ru.hh.nab.sentry.SentryInitializer;

public class SentryInitializationApplicationListener implements ApplicationListener<ApplicationContextInitializedEvent> {

  private static final String SENTRY_PROPERTIES_PREFIX = "sentry";

  @Override
  public void onApplicationEvent(ApplicationContextInitializedEvent event) {
    ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
    SentryInitializer.init(EnvironmentUtils.getSubProperties(environment, SENTRY_PROPERTIES_PREFIX));
  }
}
