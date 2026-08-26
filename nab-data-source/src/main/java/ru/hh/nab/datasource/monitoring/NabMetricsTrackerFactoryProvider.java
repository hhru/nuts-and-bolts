package ru.hh.nab.datasource.monitoring;

import java.util.Properties;
import ru.hh.metrics.MetricsSender;

public class NabMetricsTrackerFactoryProvider implements MetricsTrackerFactoryProvider<NabMetricsTrackerFactory> {
  private final String serviceName;
  private final MetricsSender metricsSender;

  public NabMetricsTrackerFactoryProvider(String serviceName, MetricsSender metricsSender) {
    this.serviceName = serviceName;
    this.metricsSender = metricsSender;
  }

  @Override
  public NabMetricsTrackerFactory create(Properties dataSourceProperties) {
    return new NabMetricsTrackerFactory(serviceName, metricsSender, dataSourceProperties);
  }
}
