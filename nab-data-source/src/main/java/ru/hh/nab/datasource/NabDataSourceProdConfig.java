package ru.hh.nab.datasource;

import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.jdbc.common.ext.OpenTelemetryJdbcExtension;
import ru.hh.nab.metrics.StatsDSender;

@Configuration
public class NabDataSourceProdConfig {
  @Bean
  DataSourceFactory dataSourceFactory(
      @Named(SERVICE_NAME) String serviceName,
      StatsDSender statsDSender,
      @Nullable OpenTelemetryJdbcExtension openTelemetryJdbcExtension,
      @Nullable DatabaseSwitcher databaseSwitcher
  ) {
    return new DataSourceFactory(
        new NabMetricsTrackerFactoryProvider(serviceName, statsDSender),
        new HealthCheckHikariDataSourceFactory(serviceName, statsDSender),
        openTelemetryJdbcExtension,
        databaseSwitcher
    );
  }
}
