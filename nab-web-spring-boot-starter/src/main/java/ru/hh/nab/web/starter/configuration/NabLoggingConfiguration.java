package ru.hh.nab.web.starter.configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.consul.ConsulTagsSupplier;
import ru.hh.nab.web.logging.LogLevelOverrideApplier;
import ru.hh.nab.web.logging.LogLevelOverrideExtension;
import ru.hh.nab.web.starter.configuration.properties.LogLevelOverrideExtensionProperties;

@Configuration
@ConditionalOnBean(LogLevelOverrideExtension.class)
@EnableConfigurationProperties(LogLevelOverrideExtensionProperties.class)
public class NabLoggingConfiguration {

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";

  @Bean
  public LogLevelOverrideApplier logLevelOverrideApplier(LogLevelOverrideExtension extension, LogLevelOverrideExtensionProperties properties) {
    return new LogLevelOverrideApplier(extension, properties.getUpdateIntervalInMinutes().toMillis(), TimeUnit.MILLISECONDS);
  }

  @Bean
  public ConsulTagsSupplier logLevelOverrideConsulTagSupplier() {
    return () -> List.of(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
  }
}
