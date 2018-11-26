package ru.hh.nab.datasource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import java.util.Properties;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;
import ru.hh.metrics.Histogram;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.monitoring.MetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_SAMPLED_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.ACQUISITION_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.CONNECTION_TIMEOUTS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.CREATION_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.USAGE_MS;
import static ru.hh.nab.testbase.NabTestConfig.TEST_SERVICE_NAME;

public class DataSourceFactoryTest {
  private static final DataSourceType TEST_DATA_SOURCE_TYPE = DataSourceType.MASTER;

  private static EmbeddedPostgres testDb;
  private static StatsDSender statsDSender;
  private static DataSourceFactory dataSourceFactory;

  @BeforeClass
  public static void setUpClass() {
    testDb = EmbeddedPostgresDataSourceFactory.getEmbeddedPostgres();
    statsDSender = mock(StatsDSender.class);
    dataSourceFactory = new DataSourceFactory(new MetricsTrackerFactoryProvider(TEST_SERVICE_NAME, statsDSender));
  }

  @Test
  public void testCreateDataSource() {
    Properties properties = createTestProperties();

    HikariDataSource dataSource = (HikariDataSource) createTestDataSource(properties);
    assertEquals(TEST_DATA_SOURCE_TYPE.getName(), dataSource.getPoolName());
  }

  @Test
  public void testCreateStatementTimeoutDataSource() {
    Properties properties = createTestProperties();
    properties.put(getProperty(STATEMENT_TIMEOUT_MS), "100");

    assertTrue(createTestDataSource(properties) instanceof StatementTimeoutDataSource);
  }

  @Test
  public void testCreateDataSourceWithMetrics() {
    Properties properties = createTestProperties();
    properties.put(getProperty(MONITORING_SEND_STATS), "true");
    properties.put(getProperty(MONITORING_LONG_CONNECTION_USAGE_MS), "10");
    properties.put(getProperty(MONITORING_SEND_SAMPLED_STATS), "true");

    HikariDataSource dataSource = (HikariDataSource) createTestDataSource(properties);
    assertNotNull(dataSource.getMetricsTrackerFactory());

    Mockito.verify(statsDSender, atLeast(1)).sendCountersPeriodically(eq(getMetricName(USAGE_MS)), any());
    Mockito.verify(statsDSender, atLeast(1)).sendCountersPeriodically(eq(getMetricName(CONNECTION_TIMEOUTS)), any());
    Mockito.verify(statsDSender, atLeast(1)).sendPercentilesPeriodically(eq(getMetricName(USAGE_MS)), (Histogram) any(), any());
    Mockito.verify(statsDSender, atLeast(1)).sendPercentilesPeriodically(eq(getMetricName(CREATION_MS)), (Histogram) any(), any());
    Mockito.verify(statsDSender, atLeast(1)).sendPercentilesPeriodically(eq(getMetricName(ACQUISITION_MS)), (Histogram) any(), any());
  }

  private static DataSource createTestDataSource(Properties properties) {
    return dataSourceFactory.create(TEST_DATA_SOURCE_TYPE, new FileSettings(properties));
  }

  private static Properties createTestProperties() {
    Properties properties = new Properties();
    String jdbcUrl = String.format(EmbeddedPostgresDataSourceFactory.DEFAULT_JDBC_URL, testDb.getPort());
    properties.put(getProperty(DataSourceSettings.JDBC_URL), jdbcUrl);
    properties.put(getProperty(DataSourceSettings.USER), EmbeddedPostgresDataSourceFactory.DEFAULT_USER);
    properties.put(getProperty(DataSourceSettings.PASSWORD), EmbeddedPostgresDataSourceFactory.DEFAULT_USER);
    return properties;
  }

  private static String getProperty(String propertyName) {
    return String.format("%s.%s", TEST_DATA_SOURCE_TYPE.getName(), propertyName);
  }

  private String getMetricName(String metricName) {
    return String.format("%s.%s.%s", TEST_SERVICE_NAME, TEST_DATA_SOURCE_TYPE.getName(), metricName);
  }
}
