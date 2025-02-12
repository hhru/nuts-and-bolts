package ru.hh.nab.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_ENABLED;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SAMPLE_POOL_USAGE_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.POOL_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.ROUTING_SECONDARY_DATASOURCE;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSource;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;

public class DataSourceFactoryTest {

  private static final String TEST_DB_JDBC_URL = "jdbc:postgresql://localhost:5432/testDb";
  private static final String TEST_DB_USER = "testUser";
  private static final String TEST_DB_PASSWORD = "testPass";
  private static final int TEST_HIKARI_MAXIMUM_POOL_SIZE = 2;
  private static final String TEST_DATA_SOURCE_TYPE = DataSourceType.MASTER;

  private static final DataSourceFactory dataSourceFactory = spy(new TestDataSourceFactory());

  @BeforeEach
  public void clearDataSourcePropertiesStorage() {
    DataSourcePropertiesStorage.clear();
  }

  @Test
  public void testCreateDataSourceWithIncompleteSettings() {
    Properties properties = createIncompleteTestProperties();
    RuntimeException exception = assertThrows(RuntimeException.class, () -> createTestDataSource(properties));
    assertTrue(exception.getMessage().contains(getProperty(POOL_SETTINGS_PREFIX)));
  }

  @Test
  public void testCreateDataSource() throws SQLException {
    assertFalse(DataSourcePropertiesStorage.isConfigured(TEST_DATA_SOURCE_TYPE));

    Properties properties = createTestProperties();
    DataSource dataSource = createTestDataSource(properties);
    assertSuccessfulUnwrap(dataSource, HikariDataSource.class);
    HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);

    verify(dataSourceFactory).checkDataSource(dataSource, TEST_DATA_SOURCE_TYPE);
    assertEquals(TEST_DB_JDBC_URL, hikariDataSource.getJdbcUrl());
    assertEquals(TEST_DB_USER, hikariDataSource.getUsername());
    assertEquals(TEST_DB_PASSWORD, hikariDataSource.getPassword());
    assertFalse(hikariDataSource.isReadOnly());
    assertEquals(24000L, hikariDataSource.getValidationTimeout());
    assertEquals(TEST_DATA_SOURCE_TYPE, hikariDataSource.getPoolName());
    assertEquals(TEST_HIKARI_MAXIMUM_POOL_SIZE, hikariDataSource.getMaximumPoolSize());
    assertTrue(DataSourcePropertiesStorage.isConfigured(TEST_DATA_SOURCE_TYPE));
    assertTrue(DataSourcePropertiesStorage.isWritableDataSource(TEST_DATA_SOURCE_TYPE));
    assertTrue(DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE).isEmpty());
  }

  @Test
  public void testCreateHealthCheckHikariDataSource() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED), "true");
    assertSuccessfulUnwrap(createTestDataSource(properties), HealthCheckHikariDataSource.class);
    assertTrue(DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE).isEmpty());
  }

  @Test
  public void testCreateDataSourceWithRoutingProperty() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED), "true");
    properties.setProperty(getProperty(ROUTING_SECONDARY_DATASOURCE), DataSourceType.READONLY);
    assertSuccessfulUnwrap(createTestDataSource(properties), HealthCheckHikariDataSource.class);

    Optional<String> secondaryDataSource = DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE);
    assertTrue(secondaryDataSource.isPresent());
    assertEquals(DataSourceType.READONLY, secondaryDataSource.get());
  }

  @Test
  public void testCreateDataSourceWithRoutingPropertyAndDisabledHealthCheck() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(ROUTING_SECONDARY_DATASOURCE), DataSourceType.READONLY);
    RuntimeException exception = assertThrows(RuntimeException.class, () -> createTestDataSource(properties));
    String expectedMessage = "Exception during master datasource initialization: if routing.failedHealthcheck.secondaryDataSource is configured, " +
        "healthcheck should be enabled";
    String actualMessage = exception.getMessage();
    assertTrue(actualMessage.contains(expectedMessage));
    assertTrue(DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE).isEmpty());
  }

  @Test
  public void testCreateStatementTimeoutDataSource() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(STATEMENT_TIMEOUT_MS), "100");
    assertSuccessfulUnwrap(createTestDataSource(properties), StatementTimeoutDataSource.class);
  }

  @Test
  public void testCreateDataSourceWithMetrics() throws SQLException {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(MONITORING_SEND_STATS), "true");
    properties.setProperty(getProperty(MONITORING_LONG_CONNECTION_USAGE_MS), "10");
    properties.setProperty(getProperty(MONITORING_SAMPLE_POOL_USAGE_STATS), "true");

    HikariDataSource dataSource = createTestDataSource(properties).unwrap(HikariDataSource.class);
    assertNotNull(dataSource.getMetricsTrackerFactory());
  }

  @Test
  public void testCheckDataSource() throws SQLException {
    Connection connection = mock(Connection.class);
    when(connection.isValid(anyInt())).thenReturn(false);
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(connection);
    DataSourceFactory dataSourceFactory = new DataSourceFactory(null, null, null);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> dataSourceFactory.checkDataSource(dataSource, TEST_DATA_SOURCE_TYPE));
    assertEquals("Invalid connection to master", exception.getMessage());

    when(dataSource.getConnection()).thenThrow(SQLException.class);
    exception = assertThrows(RuntimeException.class, () -> dataSourceFactory.checkDataSource(dataSource, TEST_DATA_SOURCE_TYPE));
    assertTrue(exception.getMessage().contains("Failed to check data source master"));
  }

  private void assertSuccessfulUnwrap(DataSource dataSource, Class<?> clazz) {
    try {
      dataSource.unwrap(clazz);
    } catch (SQLException e) {
      fail(() -> "Unable to unwrap to " + clazz.getSimpleName());
    }
  }

  private static DataSource createTestDataSource(Properties properties) {
    return dataSourceFactory.create(TEST_DATA_SOURCE_TYPE, false, properties);
  }

  private static Properties createTestProperties() {
    Properties properties = createIncompleteTestProperties();
    properties.setProperty(
        getProperty(DataSourceSettings.POOL_SETTINGS_PREFIX + ".maximumPoolSize"),
        Integer.toString(TEST_HIKARI_MAXIMUM_POOL_SIZE)
    );
    return properties;
  }

  private static Properties createIncompleteTestProperties() {
    Properties properties = new Properties();
    properties.setProperty(getProperty(DataSourceSettings.JDBC_URL), TEST_DB_JDBC_URL);
    properties.setProperty(getProperty(DataSourceSettings.USER), TEST_DB_USER);
    properties.setProperty(getProperty(DataSourceSettings.PASSWORD), TEST_DB_PASSWORD);
    return properties;
  }

  private static String getProperty(String propertyName) {
    return String.format("%s.%s", TEST_DATA_SOURCE_TYPE, propertyName);
  }
}
