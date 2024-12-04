package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;
import javax.sql.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.EntityManagerProxy;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import ru.hh.nab.datasource.annotation.DataSourceCacheMode;
import ru.hh.nab.datasource.annotation.ExecuteOnDataSource;
import ru.hh.nab.datasource.aspect.ExecuteOnDataSourceAspect;
import static ru.hh.nab.datasource.routing.DataSourceContextUnsafe.getDataSourceName;
import ru.hh.nab.jpa.JpaTestConfig;
import ru.hh.nab.testbase.jpa.JpaTestBase;

@SpringBootTest(
    classes = {JpaTestConfig.class, ExecuteOnDataSourceAspectTest.AspectConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class ExecuteOnDataSourceAspectTest extends JpaTestBase {

  private static final String WRITABLE_DATASOURCE = "writable";

  private ExecuteOnDataSourceAspect executeOnDataSourceAspect;
  private EntityManager masterEntityManager;
  private EntityManager outerReadonlyEntityManager;
  @Inject
  private TestService testService;

  @BeforeEach
  public void setUp() {
    executeOnDataSourceAspect = new ExecuteOnDataSourceAspect(
        transactionManager,
        Map.of("transactionManager", transactionManager)
    );
  }

  @Test
  public void testReadOnly() throws Throwable {
    startTransaction();
    assertEquals(MASTER, getDataSourceName());
    masterEntityManager = ((EntityManagerProxy) entityManager).getTargetEntityManager();

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyOuter());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock(DataSourceType.READONLY, false));

    assertEquals(MASTER, getDataSourceName());
    assertEquals(masterEntityManager, ((EntityManagerProxy) entityManager).getTargetEntityManager());
    rollBackTransaction();
  }

  @Test
  public void testWrite() {
    assertTransactionIsNotActive();
    testService.customWrite();
    assertTransactionIsNotActive();
  }

  @Test
  public void testThrowCheckedException() {
    String message = "test for @ExecuteOnDataSource";

    assertTransactionIsNotActive();
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

    assertTransactionIsNotActive();
    try {
      testService.throwUncheckedException(message);
      fail("Expected exception was not thrown");
    } catch (IllegalArgumentException e) {
      assertEquals(IllegalArgumentException.class, e.getClass());
      assertEquals(message, e.getMessage());
    }
  }

  private static void assertTransactionIsNotActive() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }

  private Object readonlyOuter() throws Throwable {
    assertEquals(DataSourceType.READONLY, getDataSourceName());
    outerReadonlyEntityManager = ((EntityManagerProxy) entityManager).getTargetEntityManager();
    assertNotEquals(masterEntityManager, outerReadonlyEntityManager);

    ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
    when(pjpMock.proceed()).then(invocation -> readonlyInner());
    executeOnDataSourceAspect.executeOnSpecialDataSource(pjpMock, createExecuteOnReadonlyMock(DataSourceType.READONLY, false));

    assertEquals(DataSourceType.READONLY, getDataSourceName());
    assertEquals(outerReadonlyEntityManager, ((EntityManagerProxy) entityManager).getTargetEntityManager());

    return null;
  }

  private Object readonlyInner() {
    assertEquals(DataSourceType.READONLY, getDataSourceName());
    assertEquals(outerReadonlyEntityManager, ((EntityManagerProxy) entityManager).getTargetEntityManager());
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

    private final EntityManager entityManager;

    TestService(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    @Transactional
    @ExecuteOnDataSource(dataSourceType = WRITABLE_DATASOURCE, writableTx = true)
    public void customWrite() {
      assertEquals(WRITABLE_DATASOURCE, getDataSourceName());
      assertNotNull(entityManager);
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
    DataSource readOnlyDataSource(DataSourceFactory dataSourceFactory, FileSettings fileSettings) {
      return dataSourceFactory.create(DataSourceType.READONLY, true, fileSettings);
    }

    @Bean
    DataSource writableDataSource(DataSourceFactory dataSourceFactory, FileSettings fileSettings) {
      return dataSourceFactory.create(WRITABLE_DATASOURCE, false, fileSettings);
    }

    @Bean
    TestService testService(EntityManager entityManager) {
      return new TestService(entityManager);
    }
  }
}
