package ru.hh.nab.datasource.routing;

import jakarta.inject.Inject;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.datasource.DataSourceType;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import ru.hh.nab.datasource.TestDataSourceFactory;
import ru.hh.nab.datasource.TransactionTestBase;
import static ru.hh.nab.datasource.routing.DataSourceContext.onReplica;
import static ru.hh.nab.datasource.routing.DataSourceContext.onSlowReplica;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.getDataSourceName;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.datasource.transaction.TransactionalScope;

@SpringBootTest(classes = DataSourceContextTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DataSourceContextTest extends TransactionTestBase {

  @Inject
  private TransactionalScope transactionalScope;

  @BeforeEach
  public void setUp() {
    DataSourceContextUnsafe.setDefaultMDC();
  }

  @Test
  public void testOnReplica() {
    assertIsCurrentDataSourceMaster();

    onReplica(() -> {
      assertEquals(READONLY, getDataSourceName());
      assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertIsCurrentDataSourceMaster();
  }

  @Test
  public void testOnSlowReplica() {
    assertIsCurrentDataSourceMaster();

    onSlowReplica(() -> {
      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertIsCurrentDataSourceMaster();
  }

  @Test
  public void testOnReplicaInTransaction() {
    assertThrows(IllegalStateException.class, () -> transactionalScope.write(() -> onReplica(() -> null)));
  }

  @Test
  public void testOnSlowReplicaInTransaction() {
    assertThrows(IllegalStateException.class, () -> transactionalScope.write(() -> onSlowReplica(() -> null)));
  }

  @Test
  public void testDsManageInsideTxScope() {
    Runnable dataSourceCheck = () -> assertEquals(SLOW, getDataSourceName());
    transactionalScope.read(() -> DataSourceContext.onDataSource(DataSourceType.SLOW, dataSourceCheck));
  }

  @Test
  public void testTxScopeDoesntChangeDs() {
    Runnable dataSourceCheck = () -> assertEquals(SLOW, getDataSourceName());
    DataSourceContext.onDataSource(DataSourceType.SLOW, () -> transactionalScope.read(dataSourceCheck));
  }

  private void assertIsCurrentDataSourceMaster() {
    assertEquals(MASTER, getDataSourceName());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Configuration
  @EnableTransactionManagement
  @Import(TransactionalScope.class)
  public static class TestConfiguration {

    @Bean
    public DataSourceContextTransactionManager transactionManager() {
      DataSource dataSource = new TestDataSourceFactory().createMockDataSource();
      return new DataSourceContextTransactionManager(new DataSourceTransactionManager(dataSource));
    }
  }
}
