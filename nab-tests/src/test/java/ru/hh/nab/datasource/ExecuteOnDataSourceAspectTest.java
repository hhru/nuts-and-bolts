package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceContextUnsafe.getDataSourceName;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.hibernate.transaction.DataSourceCacheMode;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSource;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;

@ContextConfiguration(classes = {HibernateTestConfig.class, ExecuteOnDataSourceAspectTest.AspectConfig.class})
public class ExecuteOnDataSourceAspectTest extends HibernateTestBase {

  private static final String WRITABLE_DATASOURCE = "writable";

  private ExecuteOnDataSourceAspect executeOnDataSourceAspect;
  private Session masterSession;
  private Session outerReadonlySession;
  @Inject
  private TestService testService;

  @BeforeEach
  public void setUp() {
    executeOnDataSourceAspect = new ExecuteOnDataSourceAspect(
        transactionManager, Map.of("transactionManager", transactionManager)
    );
  }

  @Test
  public void testReadOnly() throws Throwable {
    startTransaction();
    assertEquals(MASTER, getDataSourceName());
    masterSession = getCurrentSession();

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyOuter());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock(DataSourceType.READONLY, false));

    assertEquals(MASTER, getDataSourceName());
    assertEquals(masterSession, getCurrentSession());
    rollBackTransaction();
  }

  @Test
  public void testWrite() {
    assertHibernateIsNotInitialized();
    testService.customWrite();
    assertHibernateIsNotInitialized();
  }

  @Test
  public void testThrowCheckedException() {
    String message = "test for @ExecuteOnDataSource";

    assertHibernateIsNotInitialized();
    try {
      testService.throwCheckedException(message);
      fail("Expected exception was not thrown");
    } catch (IOException e) {
      assertEquals(IOException.class, e.getClass());
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void testThrowUncheckedException() {
    String message = "test for @ExecuteOnDataSource";

    assertHibernateIsNotInitialized();
    try {
      testService.throwUncheckedException(message);
      fail("Expected exception was not thrown");
    } catch (IllegalArgumentException e) {
      assertEquals(IllegalArgumentException.class, e.getClass());
      assertEquals(message, e.getMessage());
    }
  }

  private static void assertHibernateIsNotInitialized() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }

  private Object readonlyOuter() throws Throwable {
    assertEquals(DataSourceType.READONLY, getDataSourceName());
    outerReadonlySession = getCurrentSession();
    assertNotEquals(masterSession, outerReadonlySession);

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyInner());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock(DataSourceType.READONLY, false));

    assertEquals(DataSourceType.READONLY, getDataSourceName());
    assertEquals(outerReadonlySession, getCurrentSession());

    return null;
  }

  private Object readonlyInner() {
    assertEquals(DataSourceType.READONLY, getDataSourceName());
    assertEquals(outerReadonlySession, getCurrentSession());
    return null;
  }

  private static ExecuteOnDataSource createExecuteOnReadonlyMock(String name, boolean writableTx) {
    return new ExecuteOnDataSource() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return ExecuteOnDataSource.class;
      }

      @Override
      public boolean writableTx() {
        return writableTx;
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
      public String txManager() {
        return "transactionManager";
      }

      @Override
      public DataSourceCacheMode cacheMode() {
        return DataSourceCacheMode.NORMAL;
      }
    };
  }

  static class TestService {

    private final SessionFactory sessionFactory;

    TestService(SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
    }

    @Transactional
    @ExecuteOnDataSource(dataSourceType = WRITABLE_DATASOURCE, writableTx = true)
    public void customWrite() {
      assertEquals(WRITABLE_DATASOURCE, getDataSourceName());
      assertNotNull(sessionFactory.getCurrentSession());
      assertTrue(isSynchronizationActive());
      assertTrue(isActualTransactionActive());
    }

    @ExecuteOnDataSource(dataSourceType = WRITABLE_DATASOURCE)
    public void throwCheckedException(String message) throws IOException {
      throw new IOException(message);
    }

    @ExecuteOnDataSource(dataSourceType = WRITABLE_DATASOURCE)
    public void throwUncheckedException(String message) {
      throw new IllegalArgumentException(message);
    }
  }

  @Configuration
  static class AspectConfig {

    @Bean
    DataSource readOnlyDataSource(DataSourceFactory dataSourceFactory, FileSettings readOnlySettings) {
      return dataSourceFactory.create(DataSourceType.READONLY, true, readOnlySettings);
    }

    @Bean
    FileSettings readOnlySettings() {
      Properties properties = new Properties();
      properties.setProperty(DataSourceType.READONLY + ".pool.maximumPoolSize", "2");
      return new FileSettings(properties);
    }

    @Bean
    DataSource writableDataSource(DataSourceFactory dataSourceFactory, FileSettings writableSettings) {
      return dataSourceFactory.create(WRITABLE_DATASOURCE, false, writableSettings);
    }

    @Bean
    FileSettings writableSettings() {
      Properties properties = new Properties();
      properties.setProperty(WRITABLE_DATASOURCE + ".pool.maximumPoolSize", "2");
      return new FileSettings(properties);
    }

    @Bean
    TestService testService(SessionFactory sessionFactory) {
      return new TestService(sessionFactory);
    }
  }
}
