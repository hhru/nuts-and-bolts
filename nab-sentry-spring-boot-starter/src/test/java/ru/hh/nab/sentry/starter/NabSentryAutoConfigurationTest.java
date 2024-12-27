package ru.hh.nab.sentry.starter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import ru.hh.nab.common.spring.boot.web.servlet.SystemFilterRegistrationBean;

public class NabSentryAutoConfigurationTest {

  private static final String SENTRY_FILTER_BEAN_NAME = "sentryFilter";

  @Test
  public void testSpringContextContainsSentryFilterBeanInWebApplication() {
    WebApplicationContextRunner webApplicationContextRunner = new WebApplicationContextRunner();
    webApplicationContextRunner
        .withConfiguration(AutoConfigurations.of(NabSentryAutoConfiguration.class))
        .run(
            context -> assertThat(context)
                .hasBean(SENTRY_FILTER_BEAN_NAME)
                .getBean(SENTRY_FILTER_BEAN_NAME)
                .isInstanceOf(SystemFilterRegistrationBean.class)
        );
  }

  @Test
  public void testSpringContextDoesNotContainSentryFilterBeanInNonWebApplication() {
    ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();
    applicationContextRunner
        .withConfiguration(AutoConfigurations.of(NabSentryAutoConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean(SENTRY_FILTER_BEAN_NAME));
  }
}
