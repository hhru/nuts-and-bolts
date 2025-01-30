package ru.hh.nab.web.starter.configuration;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import ru.hh.nab.common.spring.boot.env.EnvironmentUtils;
import ru.hh.nab.consul.ConsulTagsSupplier;
import ru.hh.nab.web.logging.LogLevelOverrideApplier;
import static ru.hh.nab.web.logging.LogLevelOverrideApplier.LOG_LEVEL_OVERRIDE_EXTENSION_PROPERTIES_PREFIX;
import ru.hh.nab.web.logging.LogLevelOverrideExtension;

@Configuration
@ConditionalOnBean(LogLevelOverrideExtension.class)
public class NabLoggingConfiguration {

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";

  @Bean
  public LogLevelOverrideApplier logLevelOverrideApplier(LogLevelOverrideExtension extension, ConfigurableEnvironment environment) {
    return new LogLevelOverrideApplier(
        extension,
        EnvironmentUtils.getPropertiesStartWith(environment, LOG_LEVEL_OVERRIDE_EXTENSION_PROPERTIES_PREFIX)
    );
  }

  @Bean
  public ConsulTagsSupplier logLevelOverrideConsulTagSupplier() {
    return () -> List.of(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
  }
}
