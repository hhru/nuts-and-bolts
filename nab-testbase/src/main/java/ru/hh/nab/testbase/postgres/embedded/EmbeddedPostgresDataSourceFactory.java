package ru.hh.nab.testbase.postgres.embedded;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.hh.nab.datasource.DataSourceFactory;
import static ru.hh.nab.datasource.DataSourceSettings.JDBC_URL;
import static ru.hh.nab.datasource.DataSourceSettings.PASSWORD;
import static ru.hh.nab.datasource.DataSourceSettings.USER;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.routing.DatabaseSwitcher;

public class EmbeddedPostgresDataSourceFactory extends DataSourceFactory {
  public static final String DEFAULT_JDBC_URL = "jdbc:postgresql://${host}:${port}/${database}";
  public static final String DEFAULT_USER = "postgres";
  public static final String DEFAULT_DATABASE = "postgres";
  public static final String DEFAULT_PASSWORD = "test";

  private static final String DEFAULT_PG_IMAGE = "postgres:15.10";
  private static final String PG_IMAGE_ENV_VARIABLE = "EXT_POSTGRES_IMAGE";

  public EmbeddedPostgresDataSourceFactory() {
    super(null, null, null);
  }

  public EmbeddedPostgresDataSourceFactory(
      NabMetricsTrackerFactoryProvider nabMetricsTrackerFactoryProvider,
      HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory
  ) {
    this(nabMetricsTrackerFactoryProvider, healthCheckHikariDataSourceFactory, null);
  }

  public EmbeddedPostgresDataSourceFactory(
      NabMetricsTrackerFactoryProvider nabMetricsTrackerFactoryProvider,
      HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory,
      DatabaseSwitcher databaseSwitcher
  ) {
    super(nabMetricsTrackerFactoryProvider, healthCheckHikariDataSourceFactory, null, databaseSwitcher);
  }

  @Override
  protected DataSource createDataSource(String dataSourceName, boolean isReadonly, Properties dataSourceSettings) {
    Properties properties = new Properties();
    properties.putAll(dataSourceSettings);

    PostgreSQLContainer<?> embeddedPostgres = getEmbeddedPostgres();
    String jdbcUrl = Optional.ofNullable(dataSourceSettings.getProperty(JDBC_URL))
        .orElse(DEFAULT_JDBC_URL)
        .replace("${host}", embeddedPostgres.getHost())
        .replace("${port}", String.valueOf(embeddedPostgres.getFirstMappedPort()))
        .replace("${database}", DEFAULT_DATABASE);
    properties.setProperty(JDBC_URL, jdbcUrl);
    properties.setProperty(USER, DEFAULT_USER);
    properties.setProperty(PASSWORD, DEFAULT_PASSWORD);

    return super.createDataSource(dataSourceName, isReadonly, properties);
  }

  public static PostgreSQLContainer<?> getEmbeddedPostgres() {
    return EmbeddedPostgresSingleton.INSTANCE;
  }

  private static class EmbeddedPostgresSingleton {
    private static final PostgreSQLContainer<?> INSTANCE = createEmbeddedPostgres();

    private static PostgreSQLContainer<?> createEmbeddedPostgres() {
      String imageName = Optional.ofNullable(System.getenv(PG_IMAGE_ENV_VARIABLE)).orElse(DEFAULT_PG_IMAGE);

      PostgreSQLContainer<?> container = new PostgreSQLContainer<>(imageName)
          .withUsername(DEFAULT_USER)
          .withPassword(DEFAULT_PASSWORD);
      container.setEnv(List.of("LC_ALL=en_US.UTF-8", "LC_COLLATE=ru_RU.UTF-8", "LC_CTYPE=ru_RU.UTF-8"));
      container.setCommand(
          "postgres",
          "-c", "fsync=off",
          "-c", "autovacuum=off"
      );

      container.start();
      return container;
    }
  }
}
