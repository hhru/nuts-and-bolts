package ru.hh.nab.hibernate.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.hibernate.HibernateTestBase;
import static ru.hh.nab.hibernate.datasource.DataSourceType.MASTER;
import static ru.hh.nab.hibernate.datasource.DataSourceType.READONLY;
import static ru.hh.nab.hibernate.datasource.DataSourceType.SLOW;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceType;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.onReplica;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.onSlowReplica;

public class DataSourceContextTest extends HibernateTestBase {

  @Before
  public void setUp() {
    DataSourceContextUnsafe.setDefaultMDC();
  }

  @Test
  public void testOnReplica() {
    assertIsCurrentDataSourceMaster();

    onReplica(() -> {
      assertEquals(READONLY, getDataSourceType());
      assertEquals(READONLY.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertIsCurrentDataSourceMaster();
  }

  @Test
  public void testOnSlowReplica() {
    assertIsCurrentDataSourceMaster();

    onSlowReplica(() -> {
      assertEquals(SLOW, getDataSourceType());
      assertEquals(SLOW.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertIsCurrentDataSourceMaster();
  }

  private static void assertIsCurrentDataSourceMaster() {
    assertNull(getDataSourceType());
    assertEquals(MASTER.getName(), MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test(expected = IllegalStateException.class)
  public void testOnReplicaInTransaction() {
    PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class);
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(transactionStatus -> onReplica(() -> null));
  }

  @Test(expected = IllegalStateException.class)
  public void testOnSlowReplicaInTransaction() {
    PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class);
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(transactionStatus -> onSlowReplica(() -> null));
  }
}
