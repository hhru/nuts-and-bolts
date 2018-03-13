package ru.hh.nab.hibernate.transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.nab.hibernate.HibernateTestBase;
import ru.hh.nab.hibernate.datasource.DataSourceType;
import static ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe.getDataSourceType;

import java.lang.annotation.Annotation;

public class ExecuteOnDataSourceAspectTest extends HibernateTestBase {
  private static ExecuteOnDataSourceAspect executeOnDataSourceAspect;

  private Session masterSession;
  private Session outerReadonlySession;

  @BeforeClass
  public static void setUpClass() {
    executeOnDataSourceAspect = new ExecuteOnDataSourceAspect(transactionManager, sessionFactory);
  }

  @Before
  public void setUp() {
    startTransaction();
  }

  @After
  public void tearDown() {
    rollBackTransaction();
  }

  @Test
  public void test() throws Throwable {
    assertNull(getDataSourceType());
    masterSession = getCurrentSession();

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyOuter());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock());

    assertNull(getDataSourceType());
    assertEquals(masterSession, getCurrentSession());
  }

  private Object readonlyOuter() throws Throwable {
    assertEquals(DataSourceType.READONLY, getDataSourceType());
    outerReadonlySession = getCurrentSession();
    assertNotEquals(masterSession, outerReadonlySession);

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyInner());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock());

    assertEquals(DataSourceType.READONLY, getDataSourceType());
    assertEquals(outerReadonlySession, getCurrentSession());

    return null;
  }

  private Object readonlyInner() {
    assertEquals(DataSourceType.READONLY, getDataSourceType());
    assertEquals(outerReadonlySession, getCurrentSession());
    return null;
  }

  private static ExecuteOnDataSource createExecuteOnReadonlyMock() {
    return new ExecuteOnDataSource() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public DataSourceType dataSourceType() {
        return DataSourceType.READONLY;
      }

      @Override
      public DataSourceCacheMode cacheMode() {
        return DataSourceCacheMode.NORMAL;
      }
    };
  }
}
