package ru.hh.nab.web.starter.util;

import java.util.Properties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

public final class EnvironmentUtils {

  private EnvironmentUtils() {
  }

  public static Properties getSubProperties(Environment environment, String prefix) {
    return Binder.get(environment).bind(prefix, Properties.class).orElseGet(Properties::new);
  }
}
