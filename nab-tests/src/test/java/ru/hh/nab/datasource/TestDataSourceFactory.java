package ru.hh.nab.datasource;

import com.zaxxer.hikari.HikariConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.routing.DatabaseSwitcher;
import ru.hh.nab.metrics.StatsDSender;

class TestDataSourceFactory extends DataSourceFactory {

  private static final String TEST_SERVICE_NAME = "testService";
  private static final StatsDSender statsDSender = mock(StatsDSender.class);

  TestDataSourceFactory() {
    this(null);
  }

  TestDataSourceFactory(DatabaseSwitcher databaseSwitcher) {
    super(
        new NabMetricsTrackerFactoryProvider(TEST_SERVICE_NAME, statsDSender),
        new HealthCheckHikariDataSourceFactory(TEST_SERVICE_NAME, statsDSender),
        null,
        databaseSwitcher
    );
  }

  @Override
  DataSource createOriginalDataSource(HikariConfig hikariConfig) {
    return createMockDataSource();
  }

  DataSource createMockDataSource() {
    try {
      Connection connection = mock(Connection.class);
      DataSource dataSource = mock(DataSource.class);

      when(connection.isValid(anyInt())).thenReturn(true);
      when(connection.createStatement()).thenReturn(mock(Statement.class));
      when(dataSource.getConnection()).thenReturn(connection);
      when(dataSource.getConnection(anyString(), anyString())).thenReturn(connection);

      return dataSource;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
