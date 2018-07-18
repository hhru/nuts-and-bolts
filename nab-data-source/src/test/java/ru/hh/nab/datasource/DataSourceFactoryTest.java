package ru.hh.nab.datasource;

import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import java.util.Properties;
import javax.sql.DataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.monitoring.MonitoringDataSource;
import ru.hh.nab.datasource.monitoring.MonitoringDataSourceFactory;
import ru.hh.nab.datasource.postgres.embedded.EmbeddedPostgresDataSourceFactory;
import static ru.hh.nab.starter.NabTestConfig.TEST_SERVICE_NAME;

public class DataSourceFactoryTest {
  private static final DataSourceType TEST_DATA_SOURCE_TYPE = DataSourceType.MASTER;

  private static EmbeddedPostgres testDb;
  private static StatsDSender statsDSender;
  private static DataSourceFactory dataSourceFactory;

  @BeforeClass
  public static void setUpClass() {
    testDb = EmbeddedPostgresDataSourceFactory.Singleton.INSTANCE.getEmbeddedPostgres();
    statsDSender = mock(StatsDSender.class);
    MonitoringDataSourceFactory monitoringDataSourceFactory = new MonitoringDataSourceFactory(TEST_SERVICE_NAME, statsDSender);
    dataSourceFactory = new DataSourceFactory(monitoringDataSourceFactory);
  }

  @Test
  public void testCreatePooledDataSource() {
    Properties properties = createTestProperties();

    PoolBackedDataSource poolBackedDataSource = (PoolBackedDataSource) createTestDataSource(properties);

    assertEquals(TEST_DATA_SOURCE_TYPE.getName(), poolBackedDataSource.getIdentityToken());
  }

  @Test
  public void testCreateMonitoringDataSource() {
    Properties properties = createTestProperties();

    properties.put(getProperty(DataSourceSettings.MONITORING_SEND_STATS), "true");
    properties.put(getProperty(DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS), "10");
    properties.put(getProperty(DataSourceSettings.MONITORING_SEND_SAMPLED_STATS), "true");

    MonitoringDataSource monitoringDataSource = (MonitoringDataSource) createTestDataSource(properties);

    assertNotNull(monitoringDataSource);

    Mockito.verify(statsDSender, atLeast(1)).sendCountersPeriodically(any(), any());
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
}
