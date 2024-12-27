package ru.hh.nab.sentry;

import io.sentry.Sentry;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryInitializer.class);
  private static final String RELEASE_PROPERTY = "release";
  private static final String DSN_PROPERTY = "dsn";

  public static void init(Properties properties) {
    String dsn = properties.getProperty(DSN_PROPERTY);
    if (StringUtils.isBlank(dsn)) {
      LOGGER.warn("Sentry DSN is empty!");
    } else {
      Sentry.init(options -> {
        options.setEnableExternalConfiguration(true);
        options.setDsn(dsn);
        options.setRelease(properties.getProperty(RELEASE_PROPERTY));
      });
    }
  }
}
