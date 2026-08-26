package ru.hh.nab.datasource.healthcheck;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Set;
import ru.hh.metrics.MetricsSender;
import ru.hh.metrics.Tag;
import static ru.hh.metrics.Tag.APP_TAG_NAME;
import static ru.hh.metrics.Tag.DATASOURCE_TAG_NAME;
import ru.hh.metrics.TaggedSender;

public class HealthCheckHikariDataSourceFactory {

  private final String serviceName;
  private final MetricsSender metricsSender;

  public HealthCheckHikariDataSourceFactory(String serviceName, MetricsSender metricsSender) {
    this.serviceName = serviceName;
    this.metricsSender = metricsSender;
  }

  public HikariDataSource create(HikariConfig hikariConfig) {
    Set<Tag> tags = Set.of(
        new Tag(APP_TAG_NAME, serviceName),
        new Tag(DATASOURCE_TAG_NAME, hikariConfig.getPoolName())
    );
    TaggedSender taggedSender = new TaggedSender(metricsSender, tags);
    return new HealthCheckHikariDataSource(hikariConfig, taggedSender);
  }
}
