package ru.hh.nab.datasource.healthcheck;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Set;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import static ru.hh.nab.metrics.Tag.DATASOURCE_TAG_NAME;
import ru.hh.nab.metrics.TaggedSender;

public class HealthCheckHikariDataSourceFactory {

  private final String serviceName;
  private final StatsDSender statsDSender;

  public HealthCheckHikariDataSourceFactory(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  public HikariDataSource create(HikariConfig hikariConfig) {
    Set<Tag> tags = Set.of(
        new Tag(APP_TAG_NAME, serviceName),
        new Tag(DATASOURCE_TAG_NAME, hikariConfig.getPoolName())
    );
    TaggedSender metricsSender = new TaggedSender(statsDSender, tags);
    return new HealthCheckHikariDataSource(hikariConfig, metricsSender);
  }
}
