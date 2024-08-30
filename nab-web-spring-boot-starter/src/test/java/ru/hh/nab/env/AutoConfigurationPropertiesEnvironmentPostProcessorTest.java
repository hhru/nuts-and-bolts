package ru.hh.nab.env;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.hh.nab.env.AutoConfigurationPropertiesEnvironmentPostProcessor.PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE;
import static ru.hh.nab.env.AutoConfigurationPropertiesEnvironmentPostProcessor.SPRING_PACKAGE;
import ru.hh.nab.web.NabWebAutoConfiguration;

public class AutoConfigurationPropertiesEnvironmentPostProcessorTest {

  private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
      .withInitializer(context ->
          new AutoConfigurationPropertiesEnvironmentPostProcessor().postProcessEnvironment(context.getEnvironment(), mock(SpringApplication.class))
      );

  private final List<String> whitelistedSpringAutoConfigurations = List.of(
      ConfigurationPropertiesAutoConfiguration.class.getCanonicalName(),
      LifecycleAutoConfiguration.class.getCanonicalName(),
      PropertyPlaceholderAutoConfiguration.class.getCanonicalName(),
      ProjectInfoAutoConfiguration.class.getCanonicalName(),
      JerseyAutoConfiguration.class.getCanonicalName(),
      EmbeddedWebServerFactoryCustomizerAutoConfiguration.class.getCanonicalName(),
      ServletWebServerFactoryAutoConfiguration.class.getCanonicalName(),
      WebSocketServletAutoConfiguration.class.getCanonicalName()
  );

  @Test
  public void testExcludePropertyContainsAllBlacklistedSpringAutoConfigurations() {
    applicationContextRunner
        .run(context -> {
          List<String> excludedAutoConfigurations = asList(
              requireNonNull(context.getEnvironment().getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class))
          );

          List<String> expectedExcludedAutoConfigurations = getAllBlacklistedSpringAutoConfigurations();

          assertFalse(isEmpty(excludedAutoConfigurations));
          assertThat(excludedAutoConfigurations, containsInAnyOrder(expectedExcludedAutoConfigurations.toArray()));
        });
  }

  @Test
  public void testExcludePropertyContainsMergedUserDefinedAndBlacklistedSpringAutoConfigurations() {
    // exclude some whitelisted and custom auto configurations
    List<String> userDefinedExcludedAutoConfigurations = List.of(
        ConfigurationPropertiesAutoConfiguration.class.getCanonicalName(),
        NabWebAutoConfiguration.class.getCanonicalName()
    );
    applicationContextRunner
        .withPropertyValues("%s=%s".formatted(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, join(",", userDefinedExcludedAutoConfigurations)))
        .run(context -> {
          List<String> excludedAutoConfigurations = asList(
              requireNonNull(context.getEnvironment().getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class))
          );

          List<String> expectedExcludedAutoConfigurations = Stream
              .concat(getAllBlacklistedSpringAutoConfigurations().stream(), userDefinedExcludedAutoConfigurations.stream())
              .toList();

          assertFalse(isEmpty(excludedAutoConfigurations));
          assertThat(
              excludedAutoConfigurations,
              containsInAnyOrder(expectedExcludedAutoConfigurations.toArray())
          );
        });
  }

  private List<String> getAllBlacklistedSpringAutoConfigurations() {
    return ImportCandidates
        .load(AutoConfiguration.class, getClass().getClassLoader())
        .getCandidates()
        .stream()
        .filter(autoConfiguration -> autoConfiguration.startsWith(SPRING_PACKAGE))
        .filter(Predicate.not(whitelistedSpringAutoConfigurations::contains))
        .toList();
  }
}
