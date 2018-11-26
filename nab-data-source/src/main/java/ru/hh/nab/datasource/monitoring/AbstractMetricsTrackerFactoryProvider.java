package ru.hh.nab.datasource.monitoring;

import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import ru.hh.nab.common.properties.FileSettings;

public abstract class AbstractMetricsTrackerFactoryProvider<T extends MetricsTrackerFactory> {
  public abstract T create(FileSettings dataSourceSettings);
}
