package ru.hh.nab.hibernate.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import ru.hh.nab.datasource.DataSourceContextUnsafe;
import static ru.hh.nab.datasource.DataSourceSettings.DATASOURCE_NAME_FORMAT;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.datasource.healthcheck.HealthCheck;
import ru.hh.nab.datasource.healthcheck.HealthCheckDataSource;
import ru.hh.nab.jdbc.common.DataSourcePropertiesStorage;
import ru.hh.nab.jdbc.common.datasource.NamedDataSource;
import ru.hh.nab.jdbc.common.ext.JdbcExtension;
import ru.hh.nab.metrics.Counters;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;

public class RoutingDataSource extends AbstractRoutingDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoutingDataSource.class);
  private static final String SUCCESSFUL_SWITCHING_METRIC_NAME = "nab.db.switching.success";
  private static final String FAILED_SWITCHING_METRIC_NAME = "nab.db.switching.failure";
  private static final String PRIMARY_DATASOURCE_TAG_NAME = "primary_datasource";
  private static final String SECONDARY_DATASOURCE_TAG_NAME = "secondary_datasource";

  private final Map<String, DataSource> targetDataSources = new HashMap<>();
  private final Map<String, HealthCheck> dataSourceHealthChecks = new HashMap<>();
  private final String serviceName;
  private final Counters successfulSwitchingCounters, failedSwitchingCounters;
  private JdbcExtension jdbcExtension;

  /**
   * @deprecated Use {@link RoutingDataSource#RoutingDataSource(DataSource, String, StatsDSender)}
   */
  @Deprecated(forRemoval = true)
  public RoutingDataSource(DataSource targetDataSource) {
    this(targetDataSource, null, null);
  }

  /**
   * It's not allowed to use this constructor if application needs to work with multiple databases.
   * In this case you should use {@link RoutingDataSource#RoutingDataSource(String, StatsDSender)} and inject all dataSources via
   * - {@link RoutingDataSource#addNamedDataSource(DataSource)} - the most preferred way
   * - {@link RoutingDataSource#addDataSource(String, DataSource)}
   * - {@link RoutingDataSource#addDataSource(String, String, DataSource)}
   */
  public RoutingDataSource(DataSource targetDataSource, String serviceName, StatsDSender statsDSender) {
    this(serviceName, statsDSender);
    if (DataSourceContextUnsafe.getDatabaseSwitcher().isEmpty()) {
      addDataSource(DataSourceType.MASTER, targetDataSource);
    } else {
      throw new IllegalStateException("If your application needs to work with multiple databases " +
          "you should inject all dataSources into routingDataSource via addDataSource / addNamedDataSource methods");
    }
  }

  public RoutingDataSource(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.successfulSwitchingCounters = new Counters(50);
    this.failedSwitchingCounters = new Counters(50);

    ofNullable(statsDSender)
        .ifPresent(sender -> sender.sendPeriodically(() -> {
          statsDSender.sendCounters(SUCCESSFUL_SWITCHING_METRIC_NAME, successfulSwitchingCounters);
          statsDSender.sendCounters(FAILED_SWITCHING_METRIC_NAME, failedSwitchingCounters);
        }));
  }

  @Override
  protected String determineCurrentLookupKey() {
    String primaryDataSourceName = DataSourceContextUnsafe.getDataSourceName();
    boolean dataSourceIsHealthy = ofNullable(dataSourceHealthChecks.get(primaryDataSourceName))
        .map(healthCheck -> healthCheck.getCheckResult().healthy())
        .orElse(true);
    return dataSourceIsHealthy ? primaryDataSourceName :
        DataSourcePropertiesStorage
            .getSecondaryDataSourceName(primaryDataSourceName)
            .map(secondaryDataSourceName -> String.format(DATASOURCE_NAME_FORMAT, primaryDataSourceName, secondaryDataSourceName))
            .orElse(primaryDataSourceName);
  }

  @Nullable
  @Override
  protected DataSource determineTargetDataSource() {
    DataSource original = super.determineTargetDataSource();
    return jdbcExtension != null ? jdbcExtension.wrap(original) : original;
  }

  @Override
  public void afterPropertiesSet() {
    Map<String, HealthCheck> dataSourceHealthChecks = targetDataSources
        .values()
        .stream()
        .filter(this::isWrapperForHealthCheckDataSource)
        .map(this::unwrapHealthCheckDataSource)
        .collect(Collectors.toMap(HealthCheckDataSource::getDataSourceName, HealthCheckDataSource::getHealthCheck));

    Map<String, DataSource> secondaryDataSources = dataSourceHealthChecks
        .keySet()
        .stream()
        .map(primaryDataSourceName -> Map.entry(primaryDataSourceName, DataSourcePropertiesStorage.getSecondaryDataSourceName(primaryDataSourceName)))
        .filter(entry -> entry.getValue().isPresent())
        .collect(Collectors.toMap(
            entry -> String.format(DATASOURCE_NAME_FORMAT, entry.getKey(), entry.getValue().get()),
            entry -> {
              String primaryDataSourceName = entry.getKey();
              String secondaryDataSourceName = entry.getValue().get();
              return ofNullable(targetDataSources.get(secondaryDataSourceName))
                  .map(targetDataSource -> new SecondaryDataSourceProxy(targetDataSource, primaryDataSourceName, secondaryDataSourceName))
                  .orElseThrow(() -> new IllegalStateException("Secondary datasource %s is not configured".formatted(secondaryDataSourceName)));
            }
        ));

    this.dataSourceHealthChecks.putAll(dataSourceHealthChecks);
    this.targetDataSources.putAll(secondaryDataSources);
    setTargetDataSources(new HashMap<>(this.targetDataSources));
    super.afterPropertiesSet();
  }

  public void addDataSource(String dataSourceName, DataSource dataSource) {
    targetDataSources.put(dataSourceName, dataSource);
  }

  public void addDataSource(String databaseName, String dataSourceType, DataSource dataSource) {
    addDataSource(DATASOURCE_NAME_FORMAT.formatted(databaseName, dataSourceType), dataSource);
  }

  /**
   * Original DataSource must be wrapped with {@link NamedDataSource} otherwise IllegalArgumentException will be thrown.
   * If DataSource doesn't wrapped with {@link NamedDataSource} prefer to use
   * - {@link #addDataSource(String, DataSource)}
   * - {@link #addDataSource(String, String, DataSource)}
   */
  public void addNamedDataSource(DataSource dataSource) {
    addDataSource(
        NamedDataSource
            .getName(dataSource)
            .orElseThrow(() -> new IllegalArgumentException("Original DataSource doesn't wrapped with NamedDataSource")),
        dataSource
    );
  }

  public void setJdbcExtension(JdbcExtension jdbcExtension) {
    this.jdbcExtension = jdbcExtension;
  }

  private boolean isWrapperForHealthCheckDataSource(DataSource wrapper) {
    try {
      return wrapper.isWrapperFor(HealthCheckDataSource.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private HealthCheckDataSource unwrapHealthCheckDataSource(DataSource wrapper) {
    try {
      return wrapper.unwrap(HealthCheckDataSource.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private class SecondaryDataSourceProxy extends DelegatingDataSource {

    private final String secondaryDataSourceName;
    private final Tag[] tags;

    public SecondaryDataSourceProxy(DataSource targetDataSource, String primaryDataSourceName, String secondaryDataSourceName) {
      super(targetDataSource);
      this.secondaryDataSourceName = secondaryDataSourceName;
      this.tags = new Tag[]{
          new Tag(APP_TAG_NAME, serviceName),
          new Tag(PRIMARY_DATASOURCE_TAG_NAME, primaryDataSourceName),
          new Tag(SECONDARY_DATASOURCE_TAG_NAME, secondaryDataSourceName),
      };
    }

    @Override
    public Connection getConnection() throws SQLException {
      return this.getConnection(DataSource::getConnection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return this.getConnection(dataSource -> dataSource.getConnection(username, password));
    }

    private Connection getConnection(DataSourceConnectionSupplier dataSourceConnectionSupplier) throws SQLException {
      try {
        Connection connection = dataSourceConnectionSupplier.getFor(super.obtainTargetDataSource());
        LOGGER.warn("Switching to secondary data source {} is successful", secondaryDataSourceName);
        successfulSwitchingCounters.add(1, tags);
        return connection;
      } catch (SQLException ex) {
        LOGGER.warn("Switching to secondary data source {} is failed", secondaryDataSourceName);
        failedSwitchingCounters.add(1, tags);
        throw ex;
      }
    }
  }

  @FunctionalInterface
  private interface DataSourceConnectionSupplier {
    Connection getFor(DataSource dataSource) throws SQLException;
  }
}
