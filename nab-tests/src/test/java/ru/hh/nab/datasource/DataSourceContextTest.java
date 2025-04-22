package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import ru.hh.nab.datasource.routing.DataSourceContext;
import static ru.hh.nab.datasource.routing.DataSourceContext.onReplica;
import static ru.hh.nab.datasource.routing.DataSourceContext.onSlowReplica;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.getDataSourceName;
import ru.hh.nab.datasource.transaction.TransactionalScope;
import ru.hh.nab.testbase.transaction.TransactionTestBase;

@ContextConfiguration(classes = DataSourceTestConfig.class)
public class DataSourceContextTest extends TransactionTestBase {

  @Inject
  private PlatformTransactionManager transactionManager;
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
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    assertThrows(IllegalStateException.class, () -> transactionTemplate.execute(transactionStatus -> onReplica(() -> null)));
  }

  @Test
  public void testOnSlowReplicaInTransaction() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    assertThrows(IllegalStateException.class, () -> transactionTemplate.execute(transactionStatus -> onSlowReplica(() -> null)));
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
}
