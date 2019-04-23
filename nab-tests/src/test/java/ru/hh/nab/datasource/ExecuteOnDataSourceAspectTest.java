package ru.hh.nab.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.hibernate.transaction.DataSourceCacheMode;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSource;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;

import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceKey;

import java.lang.annotation.Annotation;

@ContextConfiguration(classes = {HibernateTestConfig.class})
public class ExecuteOnDataSourceAspectTest extends HibernateTestBase {
  private ExecuteOnDataSourceAspect executeOnDataSourceAspect;
  private Session masterSession;
  private Session outerReadonlySession;

  @Before
  public void setUp() {
    executeOnDataSourceAspect = new ExecuteOnDataSourceAspect(transactionManager, sessionFactory);
  }

  @After
  public void tearDown() {
    DataSourceType.clear();
  }

  @Test
  public void testReadOnly() throws Throwable {
    DataSourceType.registerPropertiesFor(DataSourceType.READONLY, new DataSourceType.DataSourceProperties(false));
    startTransaction();
    assertEquals(MASTER, getDataSourceKey());
    masterSession = getCurrentSession();

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyOuter());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock(DataSourceType.READONLY, true));

    assertEquals(MASTER, getDataSourceKey());
    assertEquals(masterSession, getCurrentSession());
    rollBackTransaction();
  }

  @Test
  public void testWrite() throws Throwable {
    assertHibernateIsNotInitialized();
    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> writeAssertion("writable"));
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock("writable", false));

    assertHibernateIsNotInitialized();
  }

  private Object writeAssertion(String name) {
    assertEquals(name, getDataSourceKey());
    assertNotNull(getCurrentSession());
    assertTrue(isSynchronizationActive());
    assertTrue(isActualTransactionActive());
    return null;
  }

  private static void assertHibernateIsNotInitialized() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }

  private Object readonlyOuter() throws Throwable {
    assertEquals(DataSourceType.READONLY, getDataSourceKey());
    outerReadonlySession = getCurrentSession();
    assertNotEquals(masterSession, outerReadonlySession);

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyInner());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock(DataSourceType.READONLY, true));

    assertEquals(DataSourceType.READONLY, getDataSourceKey());
    assertEquals(outerReadonlySession, getCurrentSession());

    return null;
  }

  private Object readonlyInner() {
    assertEquals(DataSourceType.READONLY, getDataSourceKey());
    assertEquals(outerReadonlySession, getCurrentSession());
    return null;
  }

  private static ExecuteOnDataSource createExecuteOnReadonlyMock(String name, boolean readOnly) {
    return new ExecuteOnDataSource() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return ExecuteOnDataSource.class;
      }

      @Override
      public boolean readOnly() {
        return readOnly;
      }

      @Override
      public String dataSourceType() {
        return name;
      }

      @Override
      public boolean overrideByRequestScope() {
        return false;
      }

      @Override
      public DataSourceCacheMode cacheMode() {
        return DataSourceCacheMode.NORMAL;
      }
    };
  }
}
