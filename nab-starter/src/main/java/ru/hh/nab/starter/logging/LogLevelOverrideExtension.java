package ru.hh.nab.starter.logging;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Extension which provides ability to override static Loggers levels with dynamic ones.
 * To activate extension simply implement it as a Spring bean.
 */
public interface LogLevelOverrideExtension {

  /**
   * How often override should be loaded and applied in minutes
   */
  int updateIntervalInMinutes();

  /**
   * Triggers your application to retrieve log level overrides.
   * @return Future with overrides map; map contains logger names and log levels as a keys and values respectively
   */
  CompletableFuture<Map<String, String>> loadLogLevelOverrides();
}
