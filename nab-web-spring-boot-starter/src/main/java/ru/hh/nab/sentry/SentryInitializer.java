package ru.hh.nab.sentry;

import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import ru.hh.nab.common.properties.FileSettings;

public class SentryInitializer implements ApplicationListener<ApplicationContextInitializedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryInitializer.class);
  private static final String SENTRY_RELEASE_ENV = "SENTRY_RELEASE";

  @Override
  public void onApplicationEvent(ApplicationContextInitializedEvent event) {
    FileSettings settings = event.getApplicationContext().getBean(FileSettings.class);
    String dsn = settings.getString("sentry.dsn");
    if (StringUtils.isBlank(dsn)) {
      LOGGER.warn("Sentry DSN is empty!");
    } else {
      Sentry.init(options -> {
        options.setEnableExternalConfiguration(true);
        options.setDsn(dsn);
        options.setRelease(System.getProperty(SENTRY_RELEASE_ENV));
      });
    }
  }
}
