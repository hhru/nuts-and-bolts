package ru.hh.nab.datasource.monitoring;

import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import ru.hh.nab.common.properties.FileSettings;

@FunctionalInterface
public interface MetricsTrackerFactoryProvider<T extends MetricsTrackerFactory> {
  T create(FileSettings dataSourceSettings);
}
