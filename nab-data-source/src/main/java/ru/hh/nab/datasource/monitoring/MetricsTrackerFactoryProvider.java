package ru.hh.nab.datasource.monitoring;

import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryConfig;
import ru.hh.nab.metrics.StatsDSender;

public class MetricsTrackerFactoryProvider extends AbstractMetricsTrackerFactoryProvider<NabMetricsTrackerFactory> {
  private final String serviceName;
  private final StatsDSender statsDSender;
  private final CompressedStackFactoryConfig compressedStackFactoryConfig;

  public MetricsTrackerFactoryProvider(String serviceName, StatsDSender statsDSender) {
    this(serviceName, statsDSender, new CompressedStackFactoryConfig());
  }

  public MetricsTrackerFactoryProvider(String serviceName, StatsDSender statsDSender, CompressedStackFactoryConfig compressedStackFactoryConfig) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
    this.compressedStackFactoryConfig = compressedStackFactoryConfig;
  }

  @Override
  public NabMetricsTrackerFactory create(FileSettings dataSourceSettings) {
    return new NabMetricsTrackerFactory(serviceName, statsDSender, compressedStackFactoryConfig, dataSourceSettings);
  }
}
