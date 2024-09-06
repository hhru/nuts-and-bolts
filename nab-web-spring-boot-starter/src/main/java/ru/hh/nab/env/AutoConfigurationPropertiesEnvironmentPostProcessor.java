package ru.hh.nab.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import ru.hh.nab.autoconfigure.AutoConfigurationWhitelist;

@Order
public class AutoConfigurationPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String PROPERTY_SOURCE_NAME = "autoConfigurationProperties";
  static final String SPRING_PACKAGE = "org.springframework";
  static final String PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    ClassLoader classLoader = this.getClass().getClassLoader();
    List<String> allAutoConfigurations = ImportCandidates.load(AutoConfiguration.class, classLoader).getCandidates();
    List<String> whitelistedAutoConfigurations = ImportCandidates.load(AutoConfigurationWhitelist.class, classLoader).getCandidates();
    List<String> blacklistedAutoConfigurations = new ArrayList<>(allAutoConfigurations);
    blacklistedAutoConfigurations.removeAll(whitelistedAutoConfigurations);
    blacklistedAutoConfigurations.removeIf(not(this::springAutoConfiguration));

    // merge blacklisted auto configurations with excluded auto configurations defined by user
    List<String> excludedAutoConfigurations = ofNullable(environment.getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class))
        .map(Arrays::asList)
        .orElseGet(List::of);
    blacklistedAutoConfigurations.addAll(excludedAutoConfigurations);

    MapPropertySource propertySource = new MapPropertySource(
        PROPERTY_SOURCE_NAME,
        Map.of(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, blacklistedAutoConfigurations)
    );
    environment.getPropertySources().addFirst(propertySource);
  }

  private boolean springAutoConfiguration(String autoConfigurationClass) {
    return autoConfigurationClass.startsWith(SPRING_PACKAGE);
  }
}
