package ru.hh.nab.web.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.hh.nab.web.logging.LogLevelOverrideApplier;
import ru.hh.nab.web.logging.LogLevelOverrideExtension;

public class NabLoggingAutoConfigurationTest {

  private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NabLoggingAutoConfiguration.class));

  @Test
  public void testSpringContextContainsAllBeans() {
    applicationContextRunner
        .withBean("logLevelOverrideExtensionBean", LogLevelOverrideExtension.class, () -> mock(LogLevelOverrideExtension.class))
        .run(context -> {
          assertThat(context).hasSingleBean(LogLevelOverrideApplier.class);
          assertThat(context).hasSingleBean(LogLevelOverrideExtensionProperties.class);
        });
  }

  @Test
  public void testSpringContextDoesNotContainLogLevelOverrideExtensionPropertiesBeanWithFailedConditions() {
    applicationContextRunner.run(context -> assertThat(context).doesNotHaveBean(LogLevelOverrideExtensionProperties.class));
  }

  @Test
  public void testSpringContextDoesNotContainLogLevelOverrideApplierBeanWithFailedConditions() {
    applicationContextRunner.run(context -> assertThat(context).doesNotHaveBean(LogLevelOverrideApplier.class));
  }
}
