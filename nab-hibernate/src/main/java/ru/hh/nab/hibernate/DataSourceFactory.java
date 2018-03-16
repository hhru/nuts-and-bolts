package ru.hh.nab.hibernate;

import com.mchange.v2.beans.BeansUtils;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.WrapperConnectionPoolDataSource;
import com.mchange.v2.log.MLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.datasource.CompressedStackFactory;
import ru.hh.nab.datasource.jdbc.MonitoringDataSource;
import ru.hh.nab.datasource.jdbc.StatementTimeoutDataSource;
import ru.hh.metrics.Counters;
import ru.hh.metrics.Histogram;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;
import ru.hh.nab.core.util.FileSettings;
import ru.hh.nab.core.util.MDC;

import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;

import static java.lang.Integer.parseInt;
import ru.hh.nab.hibernate.datasource.DataSourceType;

public class DataSourceFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

  private final String serviceName;
  private final StatsDSender statsDSender;

  public DataSourceFactory(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  public DataSource create(DataSourceType dataSourceType, FileSettings settings) {
    String dataSourceName = dataSourceType.getName();
    return createDataSource(dataSourceName, settings.getSubSettings(dataSourceName));
  }

  private DataSource createDataSource(String dataSourceName, FileSettings dataSourceSettings) {
    DataSource underlyingDataSource = createDriverManagerDataSource(dataSourceName, dataSourceSettings);

    String statementTimeoutMsVal = dataSourceSettings.getString("statementTimeoutMs");
    if (statementTimeoutMsVal != null) {
      int statementTimeoutMs = parseInt(statementTimeoutMsVal);
      if (statementTimeoutMs > 0) {
        underlyingDataSource = new StatementTimeoutDataSource(underlyingDataSource, statementTimeoutMs);
      }
    }

    underlyingDataSource = createC3P0DataSource(dataSourceName, underlyingDataSource, dataSourceSettings);

    checkDataSource(underlyingDataSource, dataSourceName);

    boolean sendStats = dataSourceSettings.getBoolean("monitoring.sendStats");
    return sendStats
        ? createMonitoringDataSource(dataSourceSettings, underlyingDataSource, dataSourceName)
        : underlyingDataSource;
  }

  private static DataSource createDriverManagerDataSource(String dataSourceName, FileSettings dataSourceSettings) {
    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(false);
    driverManagerDataSource.setJdbcUrl(dataSourceSettings.getString("jdbcUrl"));
    driverManagerDataSource.setUser(dataSourceSettings.getString("user"));
    driverManagerDataSource.setPassword(dataSourceSettings.getString("password"));
    driverManagerDataSource.setIdentityToken(dataSourceName);
    return driverManagerDataSource;
  }

  private static DataSource createC3P0DataSource(String dataSourceName, DataSource unpooledDataSource, FileSettings dataSourceSettings) {
    // We could use c3p0 DataSources.pooledDataSource(unpooledDataSource, c3p0Properties),
    // but it calls constructors with "autoregister=true" property.
    // This causes c3p0 to generate random identity token, registerDataSource datasource in jmx under this random token, which pollutes jmx metrics.
    // That is why we use constructors that allow to turn off "autoregister" property.

    try {
      Properties c3p0Properties = dataSourceSettings.getSubProperties("c3p0");

      WrapperConnectionPoolDataSource wcpds = new WrapperConnectionPoolDataSource(false);
      wcpds.setNestedDataSource(unpooledDataSource);
      wcpds.setIdentityToken(dataSourceName);
      BeansUtils.overwriteAccessiblePropertiesFromMap(
          c3p0Properties,
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
          c3p0Properties,
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

  private DataSource createMonitoringDataSource(FileSettings dataSourceSettings, DataSource underlyingDataSource, String dataSourceName) {
    int longUsageConnectionMs = dataSourceSettings.getInteger("monitoring.longConnectionUsageMs");
    boolean sendSampledStats = dataSourceSettings.getBoolean("monitoring.sendSampledStats");
    return new MonitoringDataSource(
        underlyingDataSource,
        dataSourceName,
        createConnectionGetMsConsumer(dataSourceName),
        createConnectionUsageMsConsumer(dataSourceName, longUsageConnectionMs, sendSampledStats)
    );
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

  private IntConsumer createConnectionGetMsConsumer(String dataSourceName) {
    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(getMetricName(dataSourceName, "connection.get_ms"), histogram, 50, 99, 100);
    return histogram::save;
  }

  private IntConsumer createConnectionUsageMsConsumer(String dataSourceName, int longConnectionUsageMs, boolean sendSampledStats) {

    Counters totalUsageCounter = new Counters(500);
    statsDSender.sendCountersPeriodically(getMetricName(dataSourceName, "connection.total_usage_ms"), totalUsageCounter);

    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(getMetricName(dataSourceName, "connection.usage_ms"), histogram, 50, 97, 99, 100);

    CompressedStackFactory compressedStackFactory;
    Counters sampledUsageCounters;
    if (sendSampledStats) {
      compressedStackFactory = new CompressedStackFactory(
          "ru.hh.jdbc.MonitoringConnection", "close",
          "org.glassfish.jersey.servlet.ServletContainer", "service",
          new String[]{"ru.hh."},
          new String[]{"DataSourceContext", "DataSourceFactory", "MonitoringConnection", "ExecuteOnDataSource", "TransactionManager"}
      );
      sampledUsageCounters = new Counters(2000);
      statsDSender.sendCountersPeriodically(getMetricName(dataSourceName, "connection.sampled_usage_ms"), sampledUsageCounters);

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

      String controller = MDC.getController().orElse("unknown");
      Tag controllerTag = new Tag("controller", controller);
      totalUsageCounter.add(usageMs, controllerTag);

      if (sendSampledStats && ThreadLocalRandom.current().nextInt(100) == 0) {
        String compressedStack = compressedStackFactory.create();
        sampledUsageCounters.add(usageMs, new Tag("stack", compressedStack));
      }
    };
  }

  private String getMetricName(String dataSourceName, String shortName) {
    return serviceName + '.' + dataSourceName + '.' + shortName;
  }
}
