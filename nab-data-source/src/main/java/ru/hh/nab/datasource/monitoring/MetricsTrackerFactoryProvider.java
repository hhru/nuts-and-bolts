package ru.hh.nab.datasource.monitoring;

import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import java.util.Properties;

@FunctionalInterface
public interface MetricsTrackerFactoryProvider<T extends MetricsTrackerFactory> {
  T create(Properties dataSourceProperties);
}
