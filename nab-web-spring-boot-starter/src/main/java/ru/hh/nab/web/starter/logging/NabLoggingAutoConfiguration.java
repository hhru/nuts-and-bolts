package ru.hh.nab.web.starter.logging;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.hh.nab.web.logging.LogLevelOverrideApplier;
import ru.hh.nab.web.logging.LogLevelOverrideExtension;

@AutoConfiguration
@ConditionalOnBean(LogLevelOverrideExtension.class)
@EnableConfigurationProperties(LogLevelOverrideExtensionProperties.class)
public class NabLoggingAutoConfiguration {

  @Bean
  public LogLevelOverrideApplier logLevelOverrideApplier(LogLevelOverrideExtension extension, LogLevelOverrideExtensionProperties properties) {
    return new LogLevelOverrideApplier(extension, properties.getUpdateIntervalInMinutes().toMillis(), TimeUnit.MILLISECONDS);
  }
}
