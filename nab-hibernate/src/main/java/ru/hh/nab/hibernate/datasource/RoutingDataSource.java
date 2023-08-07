package ru.hh.nab.hibernate.datasource;

import com.zaxxer.hikari.HikariConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import ru.hh.nab.datasource.DataSourcePropertiesStorage;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.datasource.NamedDataSource;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSource;
import ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe;
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
  private static final String SECONDARY_DATASOURCE_NAME_FORMAT = "%s.%s";

  private final LazyConnectionDataSource targetDataSource;
  private final Map<String, DataSource> replicas = new HashMap<>();
  private final Map<String, HealthCheckHikariDataSource.AsyncHealthCheckDecorator> dataSourceHealthChecks = new HashMap<>();
  private final String serviceName;
  private final Counters successfulSwitchingCounters, failedSwitchingCounters;
  private DataSourceProxyFactory proxyFactory;

  /**
   * @deprecated Use {@link RoutingDataSourceFactory#create(DataSource)}
   */
  @Deprecated
  public RoutingDataSource(DataSource targetDataSource) {
    this(targetDataSource, null, null);
  }

  public RoutingDataSource(DataSource targetDataSource, String serviceName, StatsDSender statsDSender) {
    // create lazy proxy. defaultAutoCommit and defaultTransactionIsolation will be determine via connection
    this.targetDataSource = new LazyConnectionDataSource(targetDataSource);
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
    String primaryDataSourceName = DataSourceContextUnsafe.getDataSourceKey();
    boolean dataSourceIsHealthy = ofNullable(dataSourceHealthChecks.get(primaryDataSourceName))
        .map(healthCheck -> healthCheck.check().isHealthy())
        .orElse(true);
    return dataSourceIsHealthy ? primaryDataSourceName :
        DataSourcePropertiesStorage.getSecondaryDataSourceName(primaryDataSourceName)
            .map(secondaryDataSourceName -> String.format(SECONDARY_DATASOURCE_NAME_FORMAT, primaryDataSourceName, secondaryDataSourceName))
            .orElse(primaryDataSourceName);
  }

  @Nullable
  @Override
  protected DataSource determineTargetDataSource() {
    DataSource original = super.determineTargetDataSource();
    return proxyFactory != null ? proxyFactory.createProxy(original) : original;
  }

  @Override
  public void afterPropertiesSet() {
    Map<String, HealthCheckHikariDataSource.AsyncHealthCheckDecorator> dataSourceHealthChecks =
        Stream.concat(Stream.of(targetDataSource), replicas.values().stream())
            .filter(this::isWrapperForHealthCheckHikariDataSource)
            .map(this::unwrapHealthCheckHikariDataSource)
            .collect(Collectors.toMap(HikariConfig::getPoolName, HealthCheckHikariDataSource::getHealthCheck));

    Map<String, DataSource> secondaryDataSources = dataSourceHealthChecks.keySet().stream()
        .map(primaryDataSourceName -> Map.entry(primaryDataSourceName, DataSourcePropertiesStorage.getSecondaryDataSourceName(primaryDataSourceName)))
        .filter(entry -> entry.getValue().isPresent())
        .collect(Collectors.toMap(
            entry -> String.format(SECONDARY_DATASOURCE_NAME_FORMAT, entry.getKey(), entry.getValue().get()),
            entry -> {
              String primaryDataSourceName = entry.getKey();
              String secondaryDataSourceName = entry.getValue().get();
              return ofNullable(replicas.get(secondaryDataSourceName))
                  .map(dataSource -> this.createSecondaryDataSourceProxy(dataSource, primaryDataSourceName, secondaryDataSourceName))
                  .orElseGet(() -> this.createSecondaryDataSourceProxy(targetDataSource, primaryDataSourceName, secondaryDataSourceName));
            }
        ));

    this.dataSourceHealthChecks.putAll(dataSourceHealthChecks);
    this.replicas.putAll(secondaryDataSources);
    HashMap<Object, Object> targetDataSources = new HashMap<>(replicas);
    targetDataSources.put(DataSourceType.MASTER, targetDataSource);
    setTargetDataSources(targetDataSources);
    super.afterPropertiesSet();
  }

  public void addDataSource(String dataSourceName, DataSource dataSource) {
    replicas.put(dataSourceName, dataSource);
  }

  /**
   * Original DataSource must be wrapped with {@link ru.hh.nab.datasource.NamedDataSource} otherwise IllegalArgumentException will be thrown.
   * If DataSource doesn't wrapped with {@link ru.hh.nab.datasource.NamedDataSource} prefer to use {@link #addDataSource(String, DataSource)}.
   */
  public void addNamedDataSource(DataSource dataSource) {
    replicas.put(
        NamedDataSource
            .getName(dataSource)
            .orElseThrow(() -> new IllegalArgumentException("Original DataSource doesn't wrapped with NamedDataSource")),
        dataSource
    );
  }

  public void setProxyFactory(DataSourceProxyFactory proxyFactory) {
    this.proxyFactory = proxyFactory;
  }

  private boolean isWrapperForHealthCheckHikariDataSource(DataSource wrapper) {
    try {
      return wrapper.isWrapperFor(HealthCheckHikariDataSource.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private HealthCheckHikariDataSource unwrapHealthCheckHikariDataSource(DataSource wrapper) {
    try {
      return wrapper.unwrap(HealthCheckHikariDataSource.class);
    } catch (SQLException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private DataSource createSecondaryDataSourceProxy(DataSource dataSource, String primaryDataSourceName, String secondaryDataSourceName) {
    return new SecondaryDataSourceProxy(dataSource, primaryDataSourceName, secondaryDataSourceName);
  }

  private DataSource createSecondaryDataSourceProxy(
      LazyConnectionDataSource defaultDataSource,
      String primaryDataSourceName,
      String secondaryDataSourceName
  ) {
    DataSource secondaryDataSource = this.createSecondaryDataSourceProxy(
        defaultDataSource.getTargetDataSource(),
        primaryDataSourceName,
        secondaryDataSourceName
    );

    // create secondaryDataSource lazy proxy via default constructor and set defaultAutoCommit and defaultTransactionIsolation parameters
    // determined on defaultDataSource lazy proxy creation. In this case connection will not be established again to defaultDataSource
    LazyConnectionDataSource lazyConnectionSecondaryDataSource = new LazyConnectionDataSource();
    lazyConnectionSecondaryDataSource.setTargetDataSource(secondaryDataSource);
    ofNullable(defaultDataSource.defaultAutoCommit()).ifPresent(lazyConnectionSecondaryDataSource::setDefaultAutoCommit);
    ofNullable(defaultDataSource.defaultTransactionIsolation()).ifPresent(lazyConnectionSecondaryDataSource::setDefaultTransactionIsolation);
    lazyConnectionSecondaryDataSource.afterPropertiesSet();
    return lazyConnectionSecondaryDataSource;
  }

  // class is created to access protected defaultAutoCommit() and defaultTransactionIsolation() methods
  private static class LazyConnectionDataSource extends LazyConnectionDataSourceProxy {

    private LazyConnectionDataSource() {
      super();
    }

    private LazyConnectionDataSource(DataSource targetDataSource) {
      super(targetDataSource);
    }

    @Override
    protected Boolean defaultAutoCommit() {
      return super.defaultAutoCommit();
    }

    @Override
    protected Integer defaultTransactionIsolation() {
      return super.defaultTransactionIsolation();
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
