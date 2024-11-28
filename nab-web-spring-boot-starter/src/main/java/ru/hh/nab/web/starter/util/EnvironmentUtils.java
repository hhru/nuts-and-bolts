package ru.hh.nab.web.starter.util;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

public final class EnvironmentUtils {

  private EnvironmentUtils() {
  }

  public static Properties getProperties(ConfigurableEnvironment environment) {
    return getPropertiesInternal(environment);
  }

  public static Properties getPropertiesStartWith(ConfigurableEnvironment environment, String prefix) {
    String fullPrefix = prefix + ".";
    Properties allProperties = getPropertiesInternal(environment);
    Properties filteredProperties = new Properties();
    allProperties
        .stringPropertyNames()
        .stream()
        .filter(propertyName -> propertyName.startsWith(fullPrefix))
        .forEach(propertyName -> filteredProperties.setProperty(propertyName, allProperties.getProperty(propertyName)));
    return filteredProperties;
  }

  public static Properties getSubProperties(ConfigurableEnvironment environment, String prefix) {
    return Binder.get(environment).bind(prefix, Properties.class).orElseGet(Properties::new);
  }

  private static Properties getPropertiesInternal(ConfigurableEnvironment environment) {
    Properties properties = new Properties();
    List<? extends EnumerablePropertySource<?>> propertySources = environment
        .getPropertySources()
        .stream()
        .filter(source -> source instanceof EnumerablePropertySource<?>)
        .map(source -> (EnumerablePropertySource<?>) source)
        .toList();
    List<String> propertyNames = propertySources
        .stream()
        .map(EnumerablePropertySource::getPropertyNames)
        .flatMap(Arrays::stream)
        .distinct()
        .toList();
    propertyNames.forEach(propertyName -> properties.setProperty(propertyName, environment.getProperty(propertyName)));
    return properties;
  }
}
