package ru.hh.nab.hibernate.transaction;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;

import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import static ru.hh.nab.datasource.DataSourceType.SLOW;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceKey;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.onReplica;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.onSlowReplica;

@ContextConfiguration(classes = {HibernateTestConfig.class, DataSourceContextTest.DataSourceContextTestConfig.class})
public class DataSourceContextTest extends HibernateTestBase {

  @Before
  public void setUp() {
    DataSourceContextUnsafe.setDefaultMDC();
  }

  @Test
  public void testOnReplica() {
    assertIsCurrentDataSourceMaster();

    onReplica(() -> {
      assertEquals(READONLY, getDataSourceKey());
      assertEquals(READONLY, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertIsCurrentDataSourceMaster();
  }

  @Test
  public void testOnSlowReplica() {
    assertIsCurrentDataSourceMaster();

    onSlowReplica(() -> {
      assertEquals(SLOW, getDataSourceKey());
      assertEquals(SLOW, MDC.get(DataSourceContextUnsafe.MDC_KEY));
      return null;
    });

    assertIsCurrentDataSourceMaster();
  }

  private static void assertIsCurrentDataSourceMaster() {
    assertEquals(MASTER, getDataSourceKey());
    assertEquals(MASTER, MDC.get(DataSourceContextUnsafe.MDC_KEY));
  }

  @Test(expected = IllegalStateException.class)
  public void testOnReplicaInTransaction() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(transactionStatus -> onReplica(() -> null));
  }

  @Test(expected = IllegalStateException.class)
  public void testOnSlowReplicaInTransaction() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(transactionStatus -> onSlowReplica(() -> null));
  }

  @Inject
  private TransactionalScope transactionalScope;

  @Test
  public void testDsManageInsideTxScope() {
    Supplier<Void> supplier = () -> {
      assertEquals(SLOW, getDataSourceKey());
      return null;
    };
    TargetMethod<Void> method = () -> onSlowReplica(supplier);
    transactionalScope.read(method);
  }

  @Test
  public void testTxScopeDoesntChangeDs() {
    TargetMethod<Void> method = () -> {
      assertEquals(SLOW, getDataSourceKey());
      return null;
    };
    Supplier<Void> supplier = () -> {
      transactionalScope.read(method);
      return null;
    };
    onSlowReplica(supplier);
  }


  @FunctionalInterface
  interface TargetMethod<T> {
    T invoke();
  }

  static class TransactionalScope {

    @Transactional(readOnly = true)
    public <T> T read(TargetMethod<T> method) {
      return method.invoke();
    }

    @Transactional
    public <T> T write(TargetMethod<T> method) {
      return method.invoke();
    }
  }

  @Configuration
  static class DataSourceContextTestConfig {
    @Bean
    TransactionalScope transactionalScope() {
      return new TransactionalScope();
    }
  }
}
