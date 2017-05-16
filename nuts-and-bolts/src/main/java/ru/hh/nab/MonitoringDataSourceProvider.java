package ru.hh.nab;

import com.google.common.base.Preconditions;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import static java.lang.Boolean.parseBoolean;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.CompressedStackFactory;
import ru.hh.jdbc.MonitoringDataSource;
import ru.hh.metrics.Counters;
import ru.hh.metrics.Histogram;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.jersey.RequestUrlFilter;

public class MonitoringDataSourceProvider implements Provider<DataSource> {

  private static final Logger logger = LoggerFactory.getLogger(MonitoringDataSourceProvider.class);

  private final String dataSourceName;
  private String serviceName;
  private Settings settings;
  private StatsDSender statsDSender;

  public MonitoringDataSourceProvider(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  @Inject
  public void setServiceName(@Named("serviceName") String serviceName) {
    this.serviceName = serviceName;
  }

  @Inject
  public void setSettings(Settings settings) {
    this.settings = settings;
  }

  @Inject
  public void setStatsDSender(StatsDSender statsDSender) {
    this.statsDSender = statsDSender;
  }

  @Override
  @SuppressWarnings({ "unchecked" })
  public DataSource get() {
    Properties c3p0Props = settings.subTree(dataSourceName + ".c3p0");
    Properties dbcpProps = settings.subTree(dataSourceName + ".dbcp");

    Properties monitoringProps = settings.subTree(dataSourceName + ".monitoring");

    Preconditions.checkState(c3p0Props.isEmpty() || dbcpProps.isEmpty(), "Both c3p0 and dbcp settings are present");
    DataSource dataSource;
    if (!c3p0Props.isEmpty()) {
      dataSource = createC3P0DataSource(dataSourceName, c3p0Props);
    } else if (!dbcpProps.isEmpty()) {
      dataSource = new BasicDataSource();
      new BeanMap(dataSource).putAll(dbcpProps);
    } else {
      throw new IllegalStateException("Neither c3p0 nor dbcp settings found");
    }

    String sendStatsString = monitoringProps.getProperty("sendStats");
    boolean sendStats;
    if (sendStatsString != null) {
      sendStats = parseBoolean(sendStatsString);
    } else {
      throw new RuntimeException("Setting " + dataSourceName + ".monitoring.sendStats must be set");
    }

    if (sendStats) {
      String longUsageConnectionMsString = monitoringProps.getProperty("longConnectionUsageMs");
      int longUsageConnectionMs;
      if (longUsageConnectionMsString != null) {
        longUsageConnectionMs = Integer.valueOf(longUsageConnectionMsString);
      } else {
        throw new RuntimeException("Setting  " + dataSourceName + ".monitoring.longConnectionUsageMs must be set");
      }

      boolean sendSampledStats = parseBoolean(monitoringProps.getProperty("sendSampledStats"));

      return new MonitoringDataSource(
          dataSource,
          dataSourceName,
          createConnectionGetMsConsumer(),
          createConnectionUsageMsConsumer(longUsageConnectionMs, sendSampledStats)
      );
    } else {
      return dataSource;
    }
  }

  private static DataSource createC3P0DataSource(String name, Map<Object, Object> properties) {
    ComboPooledDataSource ds = new ComboPooledDataSource(false);
    ds.setDataSourceName(name);
    ds.setIdentityToken(name);
    new BeanMap(ds).putAll(properties);
    C3P0Registry.reregister(ds);
    checkDataSource(ds, name);
    return ds;
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
          JerseyHttpServlet.class.getName(), "service",
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
        logger.error(message, new RuntimeException(dataSourceName + " connection usage duration exceeded"));
      }

      histogram.save(usageMs);

      String controller = MDC.get(RequestUrlFilter.CONTROLLER_MDC_KEY);
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

}
