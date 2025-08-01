package ru.hh.nab.datasource.monitoring;

import java.util.Properties;
import ru.hh.nab.metrics.StatsDSender;

public class NabMetricsTrackerFactoryProvider implements MetricsTrackerFactoryProvider<NabMetricsTrackerFactory> {
  private final String serviceName;
  private final StatsDSender statsDSender;

  public NabMetricsTrackerFactoryProvider(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  @Override
  public NabMetricsTrackerFactory create(Properties dataSourceProperties) {
    return new NabMetricsTrackerFactory(serviceName, statsDSender, dataSourceProperties);
  }
}
