package ru.hh.nab.jclient.metrics;

import java.util.Properties;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.metrics.MetricsSender;
import ru.hh.platform.utils.properties.PropertiesUtils;

public class MetricsConsumerFactory {
  private MetricsConsumerFactory() {}

  private static final MetricsConsumer NOOP_METRICS_CONSUMER = metricsProvider -> {};

  public static MetricsConsumer buildMetricsConsumer(Properties properties, String name, MetricsSender metricsSender) {
    return PropertiesUtils.getBoolean(properties, "enabled", false) ?
        new MetricsConsumerImpl(name, metricsSender) :
        NOOP_METRICS_CONSUMER;
  }
}
