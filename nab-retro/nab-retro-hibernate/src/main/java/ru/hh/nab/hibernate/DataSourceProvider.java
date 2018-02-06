package ru.hh.nab.hibernate;

import com.mchange.v2.beans.BeansUtils;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.WrapperConnectionPoolDataSource;
import com.mchange.v2.log.MLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.CompressedStackFactory;
import ru.hh.jdbc.MonitoringDataSource;
import ru.hh.jdbc.StatementTimeoutDataSource;
import ru.hh.metrics.Counters;
import ru.hh.metrics.Histogram;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

class DataSourceProvider implements Provider<DataSource> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceProvider.class);

  private final String dataSourceName;
  private Properties dataSourceProps;
  private String serviceName;
  private String controllerMdcKey;
  private String stackOuterClassExcluding;
  private String stackOuterMethodExcluding;
  private StatsDSender statsDSender;

  DataSourceProvider(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  @Inject
  public void setServiceName(@Named("serviceName") String serviceName) {
    this.serviceName = serviceName;
  }

  @Inject
  public void setSettingsProperties(@Named("settings.properties") Properties settingsProperties) {
    dataSourceProps = HibernateModule.subTree(dataSourceName, settingsProperties);
  }

  @Inject
  public void setControllerMdcKey(@Named("controller_mdc_key") String controllerMdcKey) {
    this.controllerMdcKey = controllerMdcKey;
  }

  @Inject
  public void setStackOuterClassExcluding(@Named("stack_outer_class_excluding") String stackOuterClassExcluding) {
    this.stackOuterClassExcluding = stackOuterClassExcluding;
  }

  @Inject
  public void setStackOuterMethodExcluding(@Named("stack_outer_method_excluding") String stackOuterMethodExcluding) {
    this.stackOuterMethodExcluding = stackOuterMethodExcluding;
  }

  @Inject
  public void setStatsDSender(StatsDSender statsDSender) {
    this.statsDSender = statsDSender;
  }

  @Override
  public DataSource get() {
    DataSource underlyingDataSource = createDriverManagerDataSource();

    String statementTimeoutMsVal = dataSourceProps.getProperty("statementTimeoutMs");
    if (statementTimeoutMsVal != null) {
      int statementTimeoutMs = parseInt(statementTimeoutMsVal);
      if (statementTimeoutMs > 0) {
        underlyingDataSource = new StatementTimeoutDataSource(underlyingDataSource, statementTimeoutMs);
      }
    }

    underlyingDataSource = createC3P0DataSource(underlyingDataSource);

    checkDataSource(underlyingDataSource, dataSourceName);

    boolean sendStats = parseBoolean(getNotNullProperty("monitoring.sendStats"));
    if (sendStats) {
      int longUsageConnectionMs = parseInt(getNotNullProperty("monitoring.longConnectionUsageMs"));
      boolean sendSampledStats = parseBoolean(dataSourceProps.getProperty("monitoring.sendSampledStats"));
      return new MonitoringDataSource(
          underlyingDataSource,
          dataSourceName,
          createConnectionGetMsConsumer(),
          createConnectionUsageMsConsumer(longUsageConnectionMs, sendSampledStats)
      );
    } else {
      return underlyingDataSource;
    }
  }

  private DataSource createDriverManagerDataSource() {
    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(false);
    driverManagerDataSource.setJdbcUrl(getNotNullProperty("jdbcUrl"));
    driverManagerDataSource.setUser(getNotNullProperty("user"));
    driverManagerDataSource.setPassword(getNotNullProperty("password"));
    driverManagerDataSource.setIdentityToken(dataSourceName);
    return driverManagerDataSource;
  }

  private DataSource createC3P0DataSource(DataSource unpooledDataSource) {
    // We could use c3p0 DataSources.pooledDataSource(unpooledDataSource, c3p0Properties),
    // but it calls constructors with "autoregister=true" property.
    // This causes c3p0 to generate random identity token, register datasource in jmx under this random token, which pollutes jmx metrics.
    // That is why we use constructors that allow to turn off "autoregister" property.

    try {
      Properties c3p0Props = HibernateModule.subTree("c3p0", dataSourceProps);

      WrapperConnectionPoolDataSource wcpds = new WrapperConnectionPoolDataSource(false);
      wcpds.setNestedDataSource(unpooledDataSource);
      wcpds.setIdentityToken(dataSourceName);
      BeansUtils.overwriteAccessiblePropertiesFromMap(
          c3p0Props,
          wcpds,
          false,
          null,
          true,
          MLevel.WARNING,
          MLevel.WARNING,
          true);

      PoolBackedDataSource poolBackedDataSource = new PoolBackedDataSource(false);
      poolBackedDataSource.setConnectionPoolDataSource(wcpds);
      poolBackedDataSource.setIdentityToken(dataSourceName);
      BeansUtils.overwriteAccessiblePropertiesFromMap(
          c3p0Props,
          poolBackedDataSource,
          false,
          null,
          true,
          MLevel.WARNING,
          MLevel.WARNING,
          true);
      C3P0Registry.reregister(poolBackedDataSource.unwrap(PoolBackedDataSource.class));

      return poolBackedDataSource;

    } catch (IntrospectionException | SQLException | PropertyVetoException  e) {
      throw new RuntimeException("Failed to set " + dataSourceName + ".c3p0 properties: " + e.toString(), e);
    }
  }

  private static void checkDataSource(DataSource dataSource, String dataSourceName) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(1000)) {
        throw new RuntimeException("Invalid connection to " + dataSourceName);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check data source " + dataSourceName + ": " + e.toString());
    }
  }

  private IntConsumer createConnectionGetMsConsumer() {
    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(getMetricName("connection.get_ms"), histogram, 50, 99, 100);
    return histogram::save;
  }

  private IntConsumer createConnectionUsageMsConsumer(int longConnectionUsageMs, boolean sendSampledStats) {

    Counters totalUsageCounter = new Counters(500);
    statsDSender.sendCountersPeriodically(getMetricName("connection.total_usage_ms"), totalUsageCounter);

    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(getMetricName("connection.usage_ms"), histogram, 50, 97, 99, 100);

    CompressedStackFactory compressedStackFactory;
    Counters sampledUsageCounters;
    if (sendSampledStats) {
      compressedStackFactory = new CompressedStackFactory(
              "ru.hh.jdbc.MonitoringConnection", "close",
              stackOuterClassExcluding, stackOuterMethodExcluding,
              new String[]{"ru.hh."},
              new String[]{"Interceptor", "TransactionalContext"}
      );

      sampledUsageCounters = new Counters(2000);
      statsDSender.sendCountersPeriodically(getMetricName("connection.sampled_usage_ms"), sampledUsageCounters);

    } else {
      sampledUsageCounters = null;
      compressedStackFactory = null;
    }

    return (usageMs) -> {

      if (usageMs > longConnectionUsageMs) {
        String message = String.format(
            "%s connection was used for more than %d ms (%d ms), not fatal, but should be fixed",
            dataSourceName, longConnectionUsageMs, usageMs);
        LOGGER.error(message, new RuntimeException(dataSourceName + " connection usage duration exceeded"));
      }

      histogram.save(usageMs);

      String controller = MDC.get(controllerMdcKey);
      if (controller == null) {
        controller = "unknown";
      }
      Tag controllerTag = new Tag("controller", controller);
      totalUsageCounter.add(usageMs, controllerTag);

      if (sendSampledStats && ThreadLocalRandom.current().nextInt(100) == 0) {
        String compressedStack = compressedStackFactory.create();
        sampledUsageCounters.add(usageMs, new Tag("stack", compressedStack));
      }
    };
  }

  private String getMetricName(String shortName) {
    return serviceName + '.' + dataSourceName + '.' + shortName;
  }

  private String getNotNullProperty(String key) {
    String value = dataSourceProps.getProperty(key);
    if (value == null) {
      throw new RuntimeException(dataSourceName + '.' + key + " setting NOT found");
    }
    return value;
  }

}
