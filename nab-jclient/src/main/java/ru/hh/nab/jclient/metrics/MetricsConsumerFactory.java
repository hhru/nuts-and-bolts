package ru.hh.nab.jclient.metrics;

import static java.util.Optional.ofNullable;
import java.util.Properties;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.nab.metrics.StatsDSender;

public class MetricsConsumerFactory {
  private MetricsConsumerFactory() {}

  private static final MetricsConsumer NOOP_METRICS_CONSUMER = metricsProvider -> {};

  public static MetricsConsumer buildMetricsConsumer(Properties properties, String name, StatsDSender statsDSender) {
    if (!ofNullable(properties.getProperty("enabled")).map(Boolean::parseBoolean).orElse(Boolean.FALSE)) {
      return NOOP_METRICS_CONSUMER;
    }

    return ofNullable(properties.getProperty("sendIntervalSec"))
        .map(Integer::parseInt)
        .<MetricsConsumer>map(sendIntervalSec -> new StatsDMetricsConsumer(name, statsDSender, sendIntervalSec))
        .orElse(NOOP_METRICS_CONSUMER);
  }
}
