package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.context.ContextConfiguration;
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
import ru.hh.nab.jpa.JpaTestConfig;
import ru.hh.nab.testbase.jpa.JpaTestBase;

@ContextConfiguration(classes = {JpaTestConfig.class})
public class DataSourceContextTest extends JpaTestBase {

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

  private static void assertIsCurrentDataSourceMaster() {
    assertEquals(MASTER, getDataSourceName());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));
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
