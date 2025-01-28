package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.datasource.routing.DataSourceContext;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.datasource.transaction.TransactionalScope;

@SpringBootTest(classes = DataSourceContextTransactionManagerTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DataSourceContextTransactionManagerTest extends TransactionTestBase {

  @Inject
  @Named("delegateTransactionManager")
  private PlatformTransactionManager delegateTransactionManager;
  @Inject
  private DataSourceContextTransactionManager transactionManager;
  @Inject
  private TransactionalScope transactionalScope;

  @AfterEach
  public void tearDown() {
    reset(delegateTransactionManager);
    reset(transactionManager);
  }

  @Test
  public void transactionOnMaster() {
    assertActualTransactionIsNotActive();
    transactionalScope.write(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadWriteTransaction();
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> originalTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    ArgumentCaptor<TransactionDefinition> fixedTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(transactionManager).getTransaction(originalTransactionDefinitionArgumentCaptor.capture());
    verify(delegateTransactionManager).getTransaction(fixedTransactionDefinitionArgumentCaptor.capture());

    TransactionDefinition originalTransactionDefinition = originalTransactionDefinitionArgumentCaptor.getValue();
    assertFalse(originalTransactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinition.getPropagationBehavior());

    TransactionDefinition fixedTransactionDefinition = fixedTransactionDefinitionArgumentCaptor.getValue();
    assertFalse(fixedTransactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, fixedTransactionDefinition.getPropagationBehavior());
  }

  @Test
  public void readOnlyTransactionOnMaster() {
    assertActualTransactionIsNotActive();
    transactionalScope.read(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> originalTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    ArgumentCaptor<TransactionDefinition> fixedTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(transactionManager).getTransaction(originalTransactionDefinitionArgumentCaptor.capture());
    verify(delegateTransactionManager).getTransaction(fixedTransactionDefinitionArgumentCaptor.capture());

    TransactionDefinition originalTransactionDefinition = originalTransactionDefinitionArgumentCaptor.getValue();
    assertTrue(originalTransactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinition.getPropagationBehavior());

    TransactionDefinition fixedTransactionDefinition = fixedTransactionDefinitionArgumentCaptor.getValue();
    assertTrue(fixedTransactionDefinition.isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, fixedTransactionDefinition.getPropagationBehavior());
  }

  @Test
  public void testMasterWithReadOnlyFlagInsideMaster() {
    assertActualTransactionIsNotActive();
    transactionalScope.write(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadWriteTransaction();

      transactionalScope.read(() -> {
        assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
        assertReadWriteTransaction();
      });
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> originalTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    ArgumentCaptor<TransactionDefinition> fixedTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(transactionManager, times(2)).getTransaction(originalTransactionDefinitionArgumentCaptor.capture());
    verify(delegateTransactionManager, times(2)).getTransaction(fixedTransactionDefinitionArgumentCaptor.capture());

    List<TransactionDefinition> originalTransactionDefinitions = originalTransactionDefinitionArgumentCaptor.getAllValues();
    assertEquals(2, originalTransactionDefinitions.size());
    assertFalse(originalTransactionDefinitions.get(0).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinitions.get(0).getPropagationBehavior());
    assertTrue(originalTransactionDefinitions.get(1).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinitions.get(1).getPropagationBehavior());

    List<TransactionDefinition> fixedTransactionDefinitions = fixedTransactionDefinitionArgumentCaptor.getAllValues();
    assertEquals(2, fixedTransactionDefinitions.size());
    assertFalse(fixedTransactionDefinitions.get(0).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, fixedTransactionDefinitions.get(0).getPropagationBehavior());
    assertTrue(fixedTransactionDefinitions.get(1).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, fixedTransactionDefinitions.get(1).getPropagationBehavior());
  }

  @Test
  public void testMasterInsideMasterWithReadOnlyFlag() {
    assertActualTransactionIsNotActive();
    transactionalScope.read(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();

      transactionalScope.write(() -> {
        assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
        assertReadOnlyTransaction();
      });
    });
    assertActualTransactionIsNotActive();

    ArgumentCaptor<TransactionDefinition> originalTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    ArgumentCaptor<TransactionDefinition> fixedTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(transactionManager, times(2)).getTransaction(originalTransactionDefinitionArgumentCaptor.capture());
    verify(delegateTransactionManager, times(2)).getTransaction(fixedTransactionDefinitionArgumentCaptor.capture());

    List<TransactionDefinition> originalTransactionDefinitions = originalTransactionDefinitionArgumentCaptor.getAllValues();
    assertEquals(2, originalTransactionDefinitions.size());
    assertTrue(originalTransactionDefinitions.get(0).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinitions.get(0).getPropagationBehavior());
    assertFalse(originalTransactionDefinitions.get(1).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinitions.get(1).getPropagationBehavior());

    List<TransactionDefinition> fixedTransactionDefinitions = fixedTransactionDefinitionArgumentCaptor.getAllValues();
    assertEquals(2, fixedTransactionDefinitions.size());
    assertTrue(fixedTransactionDefinitions.get(0).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, fixedTransactionDefinitions.get(0).getPropagationBehavior());
    assertTrue(fixedTransactionDefinitions.get(1).isReadOnly());
    assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, fixedTransactionDefinitions.get(1).getPropagationBehavior());
  }

  @Test
  public void testReadOnlyReplica() {
    testDataSource(DataSourceType.READONLY, true);
  }

  @Test
  public void testSlowReplica() {
    testDataSource(DataSourceType.SLOW, true);
  }

  @Test
  public void testReadOnlyReplicaInsideMasterThrowsException() {
    DataSourcePropertiesStorage.registerPropertiesFor(DataSourceType.READONLY, new DataSourcePropertiesStorage.DataSourceProperties(false));

    assertActualTransactionIsNotActive();
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> transactionalScope.write(() -> {
          assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
          assertReadWriteTransaction();

          DataSourceContext.onDataSource(DataSourceType.READONLY, () -> transactionalScope.read(() -> {}));
        })
    );
    assertActualTransactionIsNotActive();

    verify(transactionManager).rollback(any());
    assertEquals("Attempt to change data source in transaction", exception.getMessage());
  }

  @Test
  public void testReadOnlyReplicaInsideMasterWithReadOnlyFlagDoesNotThrowException() {
    DataSourcePropertiesStorage.registerPropertiesFor(DataSourceType.READONLY, new DataSourcePropertiesStorage.DataSourceProperties(false));

    assertActualTransactionIsNotActive();
    assertDoesNotThrow(() -> transactionalScope.read(() -> {
      assertEquals(DataSourceType.MASTER, DataSourceContextUnsafe.getDataSourceName());
      assertReadOnlyTransaction();

      DataSourceContext.onDataSource(DataSourceType.READONLY, () -> transactionalScope.read(() -> {
        assertEquals(DataSourceType.READONLY, DataSourceContextUnsafe.getDataSourceName());
        assertReadOnlyTransaction();
      }));
    }));
    assertActualTransactionIsNotActive();
  }

  private void testDataSource(String dataSourceName, boolean readOnly) {
    DataSourcePropertiesStorage.registerPropertiesFor(dataSourceName, new DataSourcePropertiesStorage.DataSourceProperties(!readOnly));

    assertActualTransactionIsNotActive();
    DataSourceContext.onDataSource(dataSourceName, false, () -> {
      transactionalScope.write(() -> {
        assertEquals(dataSourceName, DataSourceContextUnsafe.getDataSourceName());
        assertReadOnlyTransaction();
      });
      assertActualTransactionIsNotActive();

      ArgumentCaptor<TransactionDefinition> originalTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
      ArgumentCaptor<TransactionDefinition> fixedTransactionDefinitionArgumentCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
      verify(transactionManager).getTransaction(originalTransactionDefinitionArgumentCaptor.capture());
      verify(delegateTransactionManager).getTransaction(fixedTransactionDefinitionArgumentCaptor.capture());

      TransactionDefinition originalTransactionDefinition = originalTransactionDefinitionArgumentCaptor.getValue();
      assertFalse(originalTransactionDefinition.isReadOnly());
      assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, originalTransactionDefinition.getPropagationBehavior());

      TransactionDefinition transactionDefinition = fixedTransactionDefinitionArgumentCaptor.getValue();
      assertTrue(transactionDefinition.isReadOnly());
      assertEquals(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, transactionDefinition.getPropagationBehavior());

      return null;
    });
  }

  @Configuration
  @EnableTransactionManagement
  @Import(TransactionalScope.class)
  public static class TestConfiguration {

    @Bean
    @Named("delegateTransactionManager")
    public PlatformTransactionManager delegateTransactionManager() throws SQLException {
      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenReturn(mock(Connection.class));
      return spy(new DataSourceTransactionManager(dataSource));
    }

    @Bean
    @Primary
    public DataSourceContextTransactionManager transactionManager(PlatformTransactionManager delegateTransactionManager) {
      return spy(new DataSourceContextTransactionManager(delegateTransactionManager));
    }
  }
}
