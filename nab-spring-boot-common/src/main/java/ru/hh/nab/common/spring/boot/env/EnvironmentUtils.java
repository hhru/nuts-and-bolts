package ru.hh.nab.common.spring.boot.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import ru.hh.nab.common.properties.PropertiesUtils;

public final class EnvironmentUtils {

  private EnvironmentUtils() {
  }

  public static Properties getProperties(ConfigurableEnvironment environment) {
    return getPropertiesInternal(environment);
  }

  public static Properties getPropertiesStartWith(ConfigurableEnvironment environment, String prefix) {
    return PropertiesUtils.getPropertiesStartWith(getPropertiesInternal(environment), prefix);
  }

  public static Properties getSubProperties(ConfigurableEnvironment environment, String prefix) {
    return Binder.get(environment).bind(prefix, Properties.class).orElseGet(Properties::new);
  }

  public static Map<String, ? extends String> getPropertiesAsMap(ConfigurableEnvironment configurableEnvironment) {
    return PropertiesUtils.getAsMap(getPropertiesInternal(configurableEnvironment));
  }

  public static String getNotEmptyPropertyOrThrow(Environment environment, String propertyKey) {
    String property = environment.getRequiredProperty(propertyKey);
    if (property.isEmpty()) {
      throw new IllegalStateException(propertyKey + " in configuration must not be empty");
    }
    return property;
  }

  public static List<String> getPropertyAsStringList(Environment environment, String propertyKey) {
    return Optional
        .ofNullable(environment.getProperty(propertyKey, String[].class))
        .map(Arrays::asList)
        .orElseGet(ArrayList::new);
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
