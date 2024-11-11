package ru.hh.nab.web.starter.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import static java.util.Optional.ofNullable;
import java.util.Set;
import static java.util.stream.Collectors.toCollection;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import static ru.hh.nab.web.starter.autoconfigure.AutoConfigurationProperties.EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY;

@Order(HIGHEST_PRECEDENCE)
public class NestedAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

  private Environment environment;
  private Set<String> excludedNestedAutoConfigurations;

  @Override
  public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
    boolean[] match = new boolean[autoConfigurationClasses.length];
    for (int i = 0; i < autoConfigurationClasses.length; i++) {
      String autoConfigurationClass = autoConfigurationClasses[i];
      if (autoConfigurationClass != null) {
        Set<String> excludedNestedAutoConfigurations = getExcludedNestedAutoConfigurations();
        match[i] = !excludedNestedAutoConfigurations.contains(autoConfigurationClass);
      }
    }
    return match;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  private Set<String> getExcludedNestedAutoConfigurations() {
    if (excludedNestedAutoConfigurations == null) {
      excludedNestedAutoConfigurations = ofNullable(environment.getProperty(EXCLUDE_NESTED_AUTOCONFIGURATION_PROPERTY, String[].class))
          .stream()
          .flatMap(Arrays::stream)
          .collect(toCollection(HashSet::new));
    }
    return excludedNestedAutoConfigurations;
  }
}
