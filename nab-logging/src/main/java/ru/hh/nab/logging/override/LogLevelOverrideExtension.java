package ru.hh.nab.logging.override;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Extension which provides ability to override static Loggers levels with dynamic ones.
 * To activate extension simply implement it as a Spring bean.
 * <p>
 * Update interval can be set in service.properties via {@link LogLevelOverrideApplier#UPDATE_INTERVAL_IN_MINUTES_PROPERTY} setting.
 * If setting not specified {@link LogLevelOverrideApplier#DEFAULT_INTERVAL_IN_MINUTES} will be used by default.
 */
@FunctionalInterface
public interface LogLevelOverrideExtension {

  /**
   * Triggers your application to retrieve log level overrides.
   * The extension may throw special {@link SkipLogLevelOverrideException} to skip overriding on this call without any error.
   * @return Future with overrides map; map contains logger names and log levels as a keys and values respectively
   */
  CompletableFuture<Map<String, String>> loadLogLevelOverrides() throws SkipLogLevelOverrideException;
}
