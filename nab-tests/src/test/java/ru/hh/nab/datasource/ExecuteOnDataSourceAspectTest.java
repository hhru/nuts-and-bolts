package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import static ru.hh.nab.datasource.DataSourceType.MASTER;
import static ru.hh.nab.datasource.DataSourceType.READONLY;
import ru.hh.nab.datasource.annotation.ExecuteOnDataSource;
import ru.hh.nab.datasource.aspect.ExecuteOnDataSourceAspect;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;

@SpringBootTest(classes = ExecuteOnDataSourceAspectTest.AspectConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ExecuteOnDataSourceAspectTest extends TransactionTestBase {

  @Inject
  private ExecuteOnDataSourceAspect executeOnDataSourceAspect;
  @Inject
  private DataSourceContextTransactionManager transactionManager;
  @Inject
  private TransactionalScope transactionalScope;

  @AfterEach
  public void tearDown() {
    reset(executeOnDataSourceAspect);
    reset(transactionManager);
  }

  @Test
  public void transactionOnMaster() throws Throwable {
    assertActualTransactionIsNotActive();
    transactionalScope.writeToMaster(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadWriteTransaction();
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(executeOnDataSourceAspect).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).getTransaction(transactionDefinitionArgumentCaptor.capture());
    TransactionDefinition transactionDefinition = transactionDefinitionArgumentCaptor.getValue();
    assertFalse(transactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, transactionDefinition.getPropagationBehavior());
  }

  @Test
  public void readOnlyTransactionOnMaster() throws Throwable {
    assertActualTransactionIsNotActive();
    transactionalScope.readFromMaster(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(executeOnDataSourceAspect).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).getTransaction(transactionDefinitionArgumentCaptor.capture());
    TransactionDefinition transactionDefinition = transactionDefinitionArgumentCaptor.getValue();
    assertTrue(transactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, transactionDefinition.getPropagationBehavior());
  }

  @Test
  public void testMasterWithReadOnlyFlagInsideMaster() throws Throwable {
    assertActualTransactionIsNotActive();
    transactionalScope.writeToMaster(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadWriteTransaction();

      transactionalScope.readFromMaster(() -> {
        assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
        assertReadWriteTransaction();
      });
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(executeOnDataSourceAspect, times(2)).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).getTransaction(transactionDefinitionArgumentCaptor.capture());
    TransactionDefinition transactionDefinition = transactionDefinitionArgumentCaptor.getValue();
    assertFalse(transactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, transactionDefinition.getPropagationBehavior());
  }

  @Test
  public void testMasterInsideMasterWithReadOnlyFlag() throws Throwable {
    assertActualTransactionIsNotActive();
    transactionalScope.readFromMaster(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();

      assertDoesNotThrow(() -> transactionalScope.writeToMaster(() -> {
        assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
        assertReadOnlyTransaction();
      }));
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(executeOnDataSourceAspect, times(2)).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).getTransaction(transactionDefinitionArgumentCaptor.capture());
    TransactionDefinition transactionDefinition = transactionDefinitionArgumentCaptor.getValue();
    assertTrue(transactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, transactionDefinition.getPropagationBehavior());
  }

  @Test
  public void testReadOnlyReplica() throws Throwable {
    assertActualTransactionIsNotActive();
    transactionalScope.readFromReadOnly(() -> {
      assertEquals(READONLY, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(executeOnDataSourceAspect).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).getTransaction(transactionDefinitionArgumentCaptor.capture());
    TransactionDefinition transactionDefinition = transactionDefinitionArgumentCaptor.getValue();
    assertTrue(transactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, transactionDefinition.getPropagationBehavior());
  }

  @Test
  public void testReadOnlyReplicaInsideMasterWithReadOnlyFlagDoesNotThrowException() throws Throwable {
    DataSourcePropertiesStorage.registerPropertiesFor(DataSourceType.READONLY, new DataSourcePropertiesStorage.DataSourceProperties(false));

    assertActualTransactionIsNotActive();
    assertDoesNotThrow(() -> transactionalScope.readFromMaster(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();

      transactionalScope.readFromReadOnly(() -> {
        assertEquals(DataSourceType.READONLY, DataSourceContextUnsafe.getDataSourceName());
        assertReadOnlyTransaction();
      });
    }));
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(executeOnDataSourceAspect, times(2)).executeOnSpecialDataSource(any(), any());
    verify(transactionManager, times(2)).getTransaction(transactionDefinitionArgumentCaptor.capture());
    List<TransactionDefinition> transactionDefinitions = transactionDefinitionArgumentCaptor.getAllValues();
    assertEquals(2, transactionDefinitions.size());
    assertTrue(transactionDefinitions.get(0).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, transactionDefinitions.get(0).getPropagationBehavior());
    assertTrue(transactionDefinitions.get(1).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, transactionDefinitions.get(1).getPropagationBehavior());
  }

  @Test
  public void testThrowCheckedException() throws Throwable {
    String message = "test for @ExecuteOnDataSource";

    assertActualTransactionIsNotActive();
    IOException exception = assertThrows(
        IOException.class,
        () -> transactionalScope.writeToMaster(() -> {
          throw new IOException(message);
        })
    );
    assertActualTransactionIsNotActive();

    verify(executeOnDataSourceAspect).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).rollback(any());
    assertEquals(message, exception.getMessage());
  }

  @Test
  public void testThrowUncheckedException() throws Throwable {
    String message = "test for @ExecuteOnDataSource";

    assertActualTransactionIsNotActive();
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> transactionalScope.readFromMaster(() -> {
          throw new IllegalArgumentException(message);
        })
    );
    assertActualTransactionIsNotActive();

    verify(executeOnDataSourceAspect).executeOnSpecialDataSource(any(), any());
    verify(transactionManager).rollback(any());
    assertEquals(message, exception.getMessage());
  }

  @Configuration
  @EnableAspectJAutoProxy
  @Import(TransactionalScope.class)
  static class AspectConfig {

    @Bean
    DataSourceContextTransactionManager transactionManager() {
      TestDataSourceFactory dataSourceFactory = new TestDataSourceFactory();
      DataSource dataSource = dataSourceFactory.createMockDataSource();
      return spy(new DataSourceContextTransactionManager(new DataSourceTransactionManager(dataSource)));
    }

    @Bean
    ExecuteOnDataSourceAspect executeOnDataSourceAspect(DataSourceContextTransactionManager transactionManager) {
      return spy(new ExecuteOnDataSourceAspect(
          transactionManager,
          Map.of("transactionManager", transactionManager)
      ));
    }
  }

  static class TransactionalScope {

    @ExecuteOnDataSource(dataSourceType = MASTER, writableTx = true)
    public void writeToMaster(ThrowingRunnable runnable) throws Exception {
      runnable.run();
    }

    @ExecuteOnDataSource(dataSourceType = MASTER)
    public void readFromMaster(Runnable runnable) {
      runnable.run();
    }

    @ExecuteOnDataSource(dataSourceType = READONLY)
    public void readFromReadOnly(Runnable runnable) {
      runnable.run();
    }
  }

  interface ThrowingRunnable {
    void run() throws Exception;
  }
}
