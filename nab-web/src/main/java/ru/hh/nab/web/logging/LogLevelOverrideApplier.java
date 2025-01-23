package ru.hh.nab.web.logging;

import ch.qos.logback.classic.Level;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import java.util.concurrent.TimeUnit;
import static java.util.stream.Collectors.toMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.PropertiesUtils;

public class LogLevelOverrideApplier {

  public static final String UPDATE_INTERVAL_IN_MINUTES_PROPERTY = "logLevelOverrideExtension.updateIntervalInMinutes";
  public static final int DEFAULT_INTERVAL_IN_MINUTES = 5;

  private static final Logger LOGGER = LoggerFactory.getLogger(LogLevelOverrideApplier.class);

  private final Map<String, LogInfo> initialLogLevelsInfo = new HashMap<>();
  private final Map<String, String> previousOverrides = new HashMap<>();

  private final LogLevelOverrideExtension extension;
  private final long updateInterval;

  public LogLevelOverrideApplier(LogLevelOverrideExtension extension, Properties properties) {
    this.extension = extension;
    this.updateInterval = PropertiesUtils.getInteger(properties, UPDATE_INTERVAL_IN_MINUTES_PROPERTY, DEFAULT_INTERVAL_IN_MINUTES);
  }

  @PostConstruct
  public void run() {
    var executor = newSingleThreadScheduledExecutor((Runnable r) -> {
      Thread thread = new Thread(r, LogLevelOverrideApplier.class.getSimpleName());
      thread.setDaemon(true);
      return thread;
    });

    executor.scheduleWithFixedDelay(() -> {
      try {
        applyOverrides(getOrThrow(extension.loadLogLevelOverrides()));
      } catch (SkipLogLevelOverrideException e) {
        LOGGER.debug("Log level overriding skipped", e);
      } catch (RuntimeException e) {
        LOGGER.error("Could not apply log level overrides", e);
      }
    }, updateInterval, updateInterval, TimeUnit.MINUTES);
  }

  private void applyFilteredOverrides(Map<String, String> currentOverrides) {
    currentOverrides.forEach((logger, logLevel) -> {
      previousOverrides.put(logger, logLevel);
      LogInfo initialLogInfo = initialLogLevelsInfo.get(logger);
      var logbackLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logger);
      if (initialLogInfo == null) {
        initialLogLevelsInfo.put(logger, new LogInfo()
            .setLogLevel(Optional.ofNullable(logbackLogger.getLevel()).map(l -> l.levelStr).orElse(null))
        );
      }
      logbackLogger.setLevel(Level.toLevel(logLevel));
    });
  }

  private void applyOverrides(Map<String, String> overrides) {
    rollbackObsoleteOverrides(overrides);
    applyFilteredOverrides(filterOnlyChangedOverrides(overrides));
  }

  private void rollbackObsoleteOverrides(Map<String, String> currentOverrides) {
    Set<String> obsoleteOverrides = new HashSet<>(previousOverrides.keySet());
    obsoleteOverrides.removeAll(currentOverrides.keySet());

    obsoleteOverrides.forEach(obsoleteOverrideLogger -> {
      previousOverrides.remove(obsoleteOverrideLogger);
      var logbackLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(obsoleteOverrideLogger);
      logbackLogger.setLevel(initialLogLevelsInfo.get(obsoleteOverrideLogger).getLogLevelOptional().map(Level::toLevel).orElse(null));
      initialLogLevelsInfo.remove(obsoleteOverrideLogger);
    });
  }

  private <T> T getOrThrow(CompletableFuture<T> future) throws SkipLogLevelOverrideException {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for completable future to complete", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SkipLogLevelOverrideException) {
        throw (SkipLogLevelOverrideException) cause;
      }
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Completable future completed exceptionally", cause);
    }
  }

  private Map<String, String> filterOnlyChangedOverrides(Map<String, String> currentOverrides) {
    return currentOverrides
        .entrySet()
        .stream()
        .filter(entry -> !entry.getValue().equals(previousOverrides.get(entry.getKey())))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static class LogInfo {
    private String logLevel;

    public Optional<String> getLogLevelOptional() {
      return Optional.ofNullable(logLevel);
    }

    public LogInfo setLogLevel(String logLevel) {
      this.logLevel = logLevel;
      return this;
    }
  }
}
