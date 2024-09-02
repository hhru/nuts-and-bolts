package ru.hh.nab.logging;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.starter.logging.LogLevelOverrideApplier;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

public class NabLoggingAutoConfigurationTest {

  private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NabLoggingAutoConfiguration.class));

  @Test
  public void testSpringContextHasLogLevelOverrideApplierBean() {
    applicationContextRunner
        .withUserConfiguration(ConfigurationWithLogLevelOverrideExtensionBean.class)
        .run(context -> assertThat(context).hasSingleBean(LogLevelOverrideApplier.class));
  }

  @Test
  public void testSpringContextDoesNotHaveBeanLogLevelOverrideApplierBean() {
    applicationContextRunner
        .withUserConfiguration(ConfigurationWithoutLogLevelOverrideExtensionBean.class)
        .run(context -> assertThat(context).doesNotHaveBean(LogLevelOverrideApplier.class));
  }

  @Configuration
  public static class ConfigurationWithLogLevelOverrideExtensionBean {

    @Bean
    public LogLevelOverrideExtension testExtension() {
      return mock(LogLevelOverrideExtension.class);
    }
  }

  @Configuration
  public static class ConfigurationWithoutLogLevelOverrideExtensionBean {
  }
}
