package ru.hh.nab.datasource.routing;

import jakarta.inject.Inject;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import static ru.hh.nab.datasource.routing.DataSourceContext.onReplica;
import static ru.hh.nab.datasource.routing.DataSourceContext.onSlowReplica;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.getDataSourceName;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.datasource.transaction.TransactionalScope;

@SpringBootTest(classes = DataSourceContextTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DataSourceContextTest {

  @Inject
  private TransactionalScope transactionalScope;

  @Test
  public void testOnReplica() {
    assertEquals(MASTER, getDataSourceName());
    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));

    onReplica(() -> {
      assertEquals(READONLY, getDataSourceName());
      assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertEquals(MASTER, getDataSourceName());
    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test
  public void testOnSlowReplica() {
    assertEquals(MASTER, getDataSourceName());
    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));

    onSlowReplica(() -> {
      assertEquals(SLOW, getDataSourceName());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertEquals(MASTER, getDataSourceName());
    assertNull(MDC.get(DataSourceContextUnsafe.MDC_KEY));
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
