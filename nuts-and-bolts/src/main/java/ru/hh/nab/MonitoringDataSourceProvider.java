package ru.hh.nab;

import com.google.common.base.Preconditions;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import static java.lang.Boolean.parseBoolean;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;
import javax.inject.Inject;
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

  private Settings settings;
  private StatsDSender statsDSender;

  public MonitoringDataSourceProvider(String dataSourceName) {
    this.dataSourceName = dataSourceName;
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
              createConnectionGetMsConsumer(dataSourceName, statsDSender),
              createConnectionUsageMsConsumer(dataSourceName, longUsageConnectionMs, sendSampledStats, statsDSender)
      );
    } else {
      return dataSource;
    }
  }

  private static IntConsumer createConnectionGetMsConsumer(String dataSourceName, StatsDSender statsDSender) {
    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(dataSourceName + ".connection.get_ms", histogram, 50, 99, 100);
    return histogram::save;
  }

  private static IntConsumer createConnectionUsageMsConsumer(String dataSourceName,
                                                             int longConnectionUsageMs,
                                                             boolean sendSampledStats,
                                                             StatsDSender statsDSender) {

    Counters totalUsageCounter = new Counters(500);
    statsDSender.sendCountersPeriodically(dataSourceName + ".connection.total_usage_ms", totalUsageCounter);

    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(dataSourceName + ".connection.usage_ms", histogram, 50, 97, 99, 100);

    CompressedStackFactory compressedStackFactory;
    Counters sampledUsageCounters;
    if (sendSampledStats) {
      compressedStackFactory = new CompressedStackFactory(
          "ru.hh.jdbc.MonitoringConnection", "close",
          JerseyHttpServlet.class.getName(), "service",
          new String[]{"ru.hh."},
          new String[]{"Interceptor"}
      );

      sampledUsageCounters = new Counters(2000);
      statsDSender.sendCountersPeriodically(dataSourceName + ".connection.sampled_usage_ms", sampledUsageCounters);

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

  private static DataSource createC3P0DataSource(String name, Map<Object, Object> properties) {
    ComboPooledDataSource ds = new ComboPooledDataSource(false);
    ds.setDataSourceName(name);
    ds.setIdentityToken(name);
    new BeanMap(ds).putAll(properties);
    C3P0Registry.reregister(ds);
    return ds;
  }
}
