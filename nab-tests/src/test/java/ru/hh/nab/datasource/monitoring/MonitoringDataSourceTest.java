package ru.hh.nab.datasource.monitoring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.nab.datasource.DataSourceTestConfig;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.IntConsumer;

@ContextConfiguration(classes = {DataSourceTestConfig.class})
public class MonitoringDataSourceTest extends AbstractJUnit4SpringContextTests {

  @Inject
  private DataSource embeddedDataSource;

  @Test
  public void testGetAndReleaseConnection() throws SQLException {
    IntConsumerStub connectionGetMsConsumer = new IntConsumerStub();
    assertNull(connectionGetMsConsumer.lastValue);

    IntConsumerStub connectionUsageMsConsumer = new IntConsumerStub();
    assertNull(connectionUsageMsConsumer.lastValue);

    DataSource dataSource = createTestMonitoringDataSource(embeddedDataSource, connectionGetMsConsumer, connectionUsageMsConsumer);

    try (Connection connection = dataSource.getConnection()) {
      assertNotNull(connectionGetMsConsumer.lastValue);
      assertTrue(connectionGetMsConsumer.lastValue >= 0);

      assertTrue(connection.isValid(1));
    }

    assertNotNull(connectionUsageMsConsumer.lastValue);
    assertTrue(connectionUsageMsConsumer.lastValue >= 0);
  }

  static MonitoringDataSource createTestMonitoringDataSource(DataSource dataSource,
                                                             IntConsumerStub connectionGetMsConsumer,
                                                             IntConsumerStub connectionUsageMsConsumer) {
    return new MonitoringDataSource(dataSource, "name", connectionGetMsConsumer, connectionUsageMsConsumer);
  }

  static class IntConsumerStub implements IntConsumer {

    Integer lastValue = null;

    @Override
    public void accept(int value) {
      lastValue = value;
    }
  }
}
