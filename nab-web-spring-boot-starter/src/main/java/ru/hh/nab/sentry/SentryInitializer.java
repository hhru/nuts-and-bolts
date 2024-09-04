package ru.hh.nab.sentry;

import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

public class SentryInitializer implements ApplicationListener<ApplicationContextInitializedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryInitializer.class);
  private static final String SENTRY_RELEASE_PROPERTY = "sentry.release";
  private static final String SENTRY_DSN_PROPERTY = "sentry.dsn";

  @Override
  public void onApplicationEvent(ApplicationContextInitializedEvent event) {
    Environment environment = event.getApplicationContext().getEnvironment();
    String dsn = environment.getProperty(SENTRY_DSN_PROPERTY);
    if (StringUtils.isBlank(dsn)) {
      LOGGER.warn("Sentry DSN is empty!");
    } else {
      Sentry.init(options -> {
        options.setEnableExternalConfiguration(true);
        options.setDsn(dsn);
        options.setRelease(environment.getProperty(SENTRY_RELEASE_PROPERTY));
      });
    }
  }
}
