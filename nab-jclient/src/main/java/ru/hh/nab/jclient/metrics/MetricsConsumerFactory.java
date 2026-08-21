package ru.hh.nab.jclient.metrics;

import java.util.Properties;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.metrics.StatsDSender;
import ru.hh.platform.utils.properties.PropertiesUtils;

public class MetricsConsumerFactory {
  private MetricsConsumerFactory() {}

  private static final MetricsConsumer NOOP_METRICS_CONSUMER = metricsProvider -> {};

  public static MetricsConsumer buildMetricsConsumer(Properties properties, String name, StatsDSender statsDSender) {
    return PropertiesUtils.getBoolean(properties, "enabled", false) ?
        new StatsDMetricsConsumer(name, statsDSender) :
        NOOP_METRICS_CONSUMER;
  }
}
