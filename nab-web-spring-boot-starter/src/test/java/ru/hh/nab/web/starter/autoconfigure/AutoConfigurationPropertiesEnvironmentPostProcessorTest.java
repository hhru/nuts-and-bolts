package ru.hh.nab.web.starter.autoconfigure;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.hh.nab.web.starter.autoconfigure.AutoConfigurationProperties.EXCLUDE_AUTOCONFIGURATION_PROPERTY;
import static ru.hh.nab.web.starter.autoconfigure.AutoConfigurationProperties.EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY;
import static ru.hh.nab.web.starter.autoconfigure.AutoConfigurationPropertiesEnvironmentPostProcessor.SPRING_PACKAGE;

public class AutoConfigurationPropertiesEnvironmentPostProcessorTest {

  private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
      .withInitializer(context ->
          new AutoConfigurationPropertiesEnvironmentPostProcessor().postProcessEnvironment(context.getEnvironment(), mock(SpringApplication.class))
      );

  private final List<String> whitelistedSpringAutoConfigurations = List.of(
      ConfigurationPropertiesAutoConfiguration.class.getName(),
      LifecycleAutoConfiguration.class.getName(),
      PropertyPlaceholderAutoConfiguration.class.getName(),
      ProjectInfoAutoConfiguration.class.getName(),
      JerseyAutoConfiguration.class.getName(),
      EmbeddedWebServerFactoryCustomizerAutoConfiguration.class.getName(),
      ServletWebServerFactoryAutoConfiguration.class.getName(),
      WebSocketServletAutoConfiguration.class.getName()
  );

  private final List<String> blacklistedAutoConfigurations = ImportCandidates
      .load(AutoConfiguration.class, getClass().getClassLoader())
      .getCandidates()
      .stream()
      .filter(autoConfiguration -> autoConfiguration.startsWith(SPRING_PACKAGE))
      .filter(Predicate.not(whitelistedSpringAutoConfigurations::contains))
      .toList();

  private final List<String> blacklistedNestedAutoConfigurations = List.of(
      "org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration$JacksonResourceConfigCustomizer",
      "org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration$JacksonResourceConfigCustomizer$JaxbObjectMapperCustomizer"
  );

  @Test
  public void testExcludePropertyContainsAllBlacklistedSpringAutoConfigurations() {
    applicationContextRunner
        .run(context -> {
          List<String> excludedAutoConfigurations = getPropertyValue(context, EXCLUDE_AUTOCONFIGURATION_PROPERTY);

          assertFalse(isEmpty(excludedAutoConfigurations));
          assertThat(excludedAutoConfigurations, containsInAnyOrder(blacklistedAutoConfigurations.toArray()));
        });
  }

  @Test
  public void testExcludePropertyContainsMergedUserDefinedAndBlacklistedSpringAutoConfigurations() {
    // exclude some whitelisted and custom auto configurations
    List<String> userDefinedExcludedAutoConfigurations = List.of(
        ConfigurationPropertiesAutoConfiguration.class.getName(),
        TestAutoConfiguration.class.getName()
    );
    applicationContextRunner
        .withPropertyValues("%s=%s".formatted(EXCLUDE_AUTOCONFIGURATION_PROPERTY, join(",", userDefinedExcludedAutoConfigurations)))
        .run(context -> {
          List<String> excludedAutoConfigurations = getPropertyValue(context, EXCLUDE_AUTOCONFIGURATION_PROPERTY);

          List<String> mergedExcludedAutoConfigurations = Stream
              .concat(blacklistedAutoConfigurations.stream(), userDefinedExcludedAutoConfigurations.stream())
              .toList();

          assertFalse(isEmpty(excludedAutoConfigurations));
          assertThat(
              excludedAutoConfigurations,
              containsInAnyOrder(mergedExcludedAutoConfigurations.toArray())
          );
        });
  }

  @Test
  public void testExcludeNestedPropertyContainsAllBlacklistedNestedAutoConfigurations() {
    applicationContextRunner
        .run(context -> {
          List<String> excludedNestedAutoConfigurations = getPropertyValue(context, EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY);

          assertFalse(isEmpty(excludedNestedAutoConfigurations));
          assertThat(excludedNestedAutoConfigurations, containsInAnyOrder(blacklistedNestedAutoConfigurations.toArray()));
        });
  }

  @Test
  public void testExcludeNestedPropertyContainsMergedUserDefinedAndBlacklistedNestedAutoConfigurations() {
    // exclude some nested auto configurations
    List<String> userDefinedExcludedNestedAutoConfigurations = List.of(
        TestAutoConfiguration.NestedConfiguration.class.getName()
    );
    applicationContextRunner
        .withPropertyValues("%s=%s".formatted(EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY, join(",", userDefinedExcludedNestedAutoConfigurations)))
        .run(context -> {
          List<String> excludedNestedAutoConfigurations = getPropertyValue(context, EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY);

          List<String> mergedExcludedNestedAutoConfigurations = Stream
              .concat(blacklistedNestedAutoConfigurations.stream(), userDefinedExcludedNestedAutoConfigurations.stream())
              .toList();

          assertFalse(isEmpty(excludedNestedAutoConfigurations));
          assertThat(excludedNestedAutoConfigurations, containsInAnyOrder(mergedExcludedNestedAutoConfigurations.toArray()));
        });
  }

  private List<String> getPropertyValue(ApplicationContext applicationContext, String property) {
    return asList(requireNonNull(applicationContext.getEnvironment().getProperty(property, String[].class)));
  }

  @AutoConfiguration
  private static class TestAutoConfiguration {
    @Configuration
    private static class NestedConfiguration {
    }
  }
}
