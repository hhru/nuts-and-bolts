package ru.hh.nab.common.spring.boot.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.Set;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import static ru.hh.nab.common.spring.boot.autoconfigure.AutoConfigurationProperties.EXCLUDE_AUTOCONFIGURATION_PROPERTY;
import static ru.hh.nab.common.spring.boot.autoconfigure.AutoConfigurationProperties.EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY;

/**
 * Environment post processor which is responsible for calculation of autoconfiguration blacklist and registration of property source with auto
 * configuration properties (see {@link AutoConfigurationProperties}).
 * <p>
 * Property {@link AutoConfigurationProperties#EXCLUDE_AUTOCONFIGURATION_PROPERTY} is used in {@link AutoConfigurationImportSelector} in order to
 * get and filter excluded auto configurations.
 * <p>
 * Property {@link AutoConfigurationProperties#EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY} is used in {@link NestedAutoConfigurationImportFilter} in
 * order to get and filter excluded nested auto configurations.
 */
@Order
public class AutoConfigurationPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String PROPERTY_SOURCE_NAME = "autoConfigurationProperties";
  static final String SPRING_PACKAGE = "org.springframework";

  private final ClassLoader classLoader;
  private final List<String> whitelistedAutoConfigurations;

  public AutoConfigurationPropertiesEnvironmentPostProcessor() {
    this.classLoader = this.getClass().getClassLoader();
    this.whitelistedAutoConfigurations = ImportCandidates.load(AutoConfigurationWhitelist.class, classLoader).getCandidates();
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Set<String> excludedAutoConfigurations = getExcludedAutoConfigurations(environment);
    Set<String> excludedNestedAutoConfigurations = getExcludedNestedAutoConfigurations(environment);
    MapPropertySource propertySource = new MapPropertySource(
        PROPERTY_SOURCE_NAME,
        Map.of(
            EXCLUDE_AUTOCONFIGURATION_PROPERTY, excludedAutoConfigurations,
            EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY, excludedNestedAutoConfigurations
        )
    );
    environment.getPropertySources().addFirst(propertySource);
  }

  private Set<String> getExcludedAutoConfigurations(ConfigurableEnvironment environment) {
    Set<String> blacklistedAutoConfigurations = loadConfigurations(AutoConfiguration.class);
    whitelistedAutoConfigurations.forEach(blacklistedAutoConfigurations::remove);
    blacklistedAutoConfigurations.removeIf(not(this::springAutoConfiguration));

    // merge blacklisted auto configurations with excluded auto configurations defined by user
    Set<String> excludedAutoConfigurations = getPropertyValue(environment, EXCLUDE_AUTOCONFIGURATION_PROPERTY);
    blacklistedAutoConfigurations.addAll(excludedAutoConfigurations);

    return blacklistedAutoConfigurations;
  }

  private Set<String> getExcludedNestedAutoConfigurations(ConfigurableEnvironment environment) {
    Set<String> blacklistedNestedAutoConfigurations = loadConfigurations(NestedAutoConfigurationBlacklist.class);
    whitelistedAutoConfigurations.forEach(blacklistedNestedAutoConfigurations::remove);

    // merge blacklisted auto configurations with excluded nested auto configurations defined by user
    Set<String> excludedNestedAutoConfigurations = getPropertyValue(environment, EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY);
    blacklistedNestedAutoConfigurations.addAll(excludedNestedAutoConfigurations);

    return blacklistedNestedAutoConfigurations;
  }

  private Set<String> loadConfigurations(Class<?> annotation) {
    return new HashSet<>(ImportCandidates.load(annotation, classLoader).getCandidates());
  }

  private Set<String> getPropertyValue(ConfigurableEnvironment environment, String property) {
    return ofNullable(environment.getProperty(property, String[].class))
        .stream()
        .flatMap(Arrays::stream)
        .collect(toCollection(HashSet::new));
  }

  private boolean springAutoConfiguration(String autoConfigurationClass) {
    return autoConfigurationClass.startsWith(SPRING_PACKAGE);
  }
}
