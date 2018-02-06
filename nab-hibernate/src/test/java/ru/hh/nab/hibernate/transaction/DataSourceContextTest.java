package ru.hh.nab.hibernate.transaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.hibernate.HibernateCommonConfig;
import ru.hh.nab.hibernate.datasource.DataSourceType;
import ru.hh.nab.testbase.CoreTestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.onReplica;
import static ru.hh.nab.core.util.MDC.DATA_SOURCE_MDC_KEY;

@ContextConfiguration(classes = {HibernateTestConfig.class, HibernateCommonConfig.class})
public class DataSourceContextTest extends CoreTestBase {

  @BeforeClass
  public static void setUpDataSourceContextTest() {
    DataSourceContext.enableTransactionCheck();
  }

  @Before
  public void setUp() {
    DataSourceContext.setDefaultMDC();
  }

  @Test
  public void testOnReplica() {
    assertNull(DataSourceContext.getDataSourceType());
    Assert.assertEquals(DataSourceType.DEFAULT.getId(), MDC.get(DATA_SOURCE_MDC_KEY));

    onReplica(() -> {
      assertEquals(DataSourceType.REPLICA, DataSourceContext.getDataSourceType());
      assertEquals(DataSourceType.REPLICA.getId(), MDC.get(DATA_SOURCE_MDC_KEY));
      return null;
    });

    assertNull(DataSourceContext.getDataSourceType());
    assertEquals(DataSourceType.DEFAULT.getId(), MDC.get(DATA_SOURCE_MDC_KEY));
  }

  @Test(expected = IllegalStateException.class)
  public void testOnReplicaInTransaction() {
    PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(transactionStatus -> onReplica(() -> null));
  }
}
