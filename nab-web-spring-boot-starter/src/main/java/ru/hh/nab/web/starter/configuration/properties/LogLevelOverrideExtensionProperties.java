package ru.hh.nab.web.starter.configuration.properties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties(prefix = "log-level-override-extension")
public class LogLevelOverrideExtensionProperties {

  @DurationUnit(ChronoUnit.MINUTES)
  private Duration updateIntervalInMinutes = Duration.ofMinutes(5);

  public Duration getUpdateIntervalInMinutes() {
    return updateIntervalInMinutes;
  }

  public void setUpdateIntervalInMinutes(Duration updateIntervalInMinutes) {
    this.updateIntervalInMinutes = updateIntervalInMinutes;
  }
}
