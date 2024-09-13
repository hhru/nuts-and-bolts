package ru.hh.nab.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;
import ru.hh.nab.jpa.JpaTestConfig;
import ru.hh.nab.jpa.model.TestEntity;
import ru.hh.nab.testbase.jpa.JpaTestBase;

@SpringBootTest(classes = JpaTestConfig.class)
public class DataSourceContextTransactionManagerTest extends JpaTestBase {
  private TestEntity existingTestEntity;

  @BeforeEach
  public void setUpTestBaseClass() {
    DataSourcePropertiesStorage.clear();
    startTransaction();
    this.existingTestEntity = createTestEntity();
    commitTransaction();
  }

  @Test
  public void noDefaultSynchronizationInTests() {
    assertThrows(IllegalStateException.class, () -> ((EntityManagerProxy) entityManager).getTargetEntityManager());
  }

  @Test
  public void transactionOnMaster() {
    TransactionStatus transactionStatus = createTransaction(false);

    assertReadWriteTransaction();

    TestEntity newTestEntity = createTestEntity();
    TestEntity newTestEntityFromDb = entityManager.find(TestEntity.class, newTestEntity.getId());
    assertNotNull(newTestEntityFromDb);

    transactionManager.rollback(transactionStatus);
    assertActualTransactionIsNotActive();

    transactionStatus = createTransaction(false);
    assertNull(entityManager.find(TestEntity.class, newTestEntity.getId()));
    transactionManager.rollback(transactionStatus);
  }

  @Test
  public void readOnlyTransactionOnMasterShouldOnlyInitializePersistenceProvider() {
    TransactionStatus readOnlyTransactionStatus = createTransaction(true);
    assertReadOnlyMode();

    assertFalse(entityManager.contains(existingTestEntity));

    TestEntity testEntityFromDb = entityManager.find(TestEntity.class, existingTestEntity.getId());
    assertNotNull(testEntityFromDb);
    assertTrue(entityManager.contains(testEntityFromDb));
    assertEquals(existingTestEntity.getId(), testEntityFromDb.getId().intValue());

    transactionManager.commit(readOnlyTransactionStatus);

    assertActualTransactionIsNotActive();
  }

  @Test
  public void testMasterWithReadOnlyFlagInsideMaster() {
    TransactionStatus outerReadWriteTransactionStatus = createTransaction(false);

    TransactionStatus innerReadOnlyTransactionStatus = createTransaction(true);

    assertReadWriteTransaction();

    assertFalse(entityManager.contains(existingTestEntity));
    assertNotNull(entityManager.find(TestEntity.class, existingTestEntity.getId()));

    transactionManager.commit(innerReadOnlyTransactionStatus);

    assertReadWriteTransaction();

    TestEntity newTestEntity = createTestEntity();
    assertNotNull(entityManager.find(TestEntity.class, newTestEntity.getId()));

    transactionManager.rollback(outerReadWriteTransactionStatus);

    assertActualTransactionIsNotActive();
  }

  @Test
  public void testMasterInsideMasterWithReadOnlyFlag() {
    TransactionStatus outerReadOnlyTransactionStatus = createTransaction(true);

    assertReadOnlyMode();

    TransactionStatus innerReadWriteTransactionStatus = createTransaction(false);

    assertReadOnlyMode();

    transactionManager.commit(innerReadWriteTransactionStatus);

    assertReadOnlyMode();

    transactionManager.commit(outerReadOnlyTransactionStatus);

    assertActualTransactionIsNotActive();
  }

  @Test
  public void testDataSource() {
    testDataSource(DataSourceType.READONLY, true);
  }

  @Test
  public void testSlowReplica() {
    testDataSource(DataSourceType.SLOW, true);
  }

  private void testDataSource(String dataSourceName, boolean readOnly) {
    assertActualTransactionIsNotActive();
    DataSourcePropertiesStorage.registerPropertiesFor(dataSourceName, new DataSourcePropertiesStorage.DataSourceProperties(!readOnly));
    DataSourceContextUnsafe.executeOn(dataSourceName, false, () -> {
      TransactionStatus transactionStatus = createTransaction(false);

      assertReadOnlyMode();

      assertFalse(entityManager.contains(existingTestEntity));

      TestEntity testEntity = entityManager.find(TestEntity.class, existingTestEntity.getId());
      assertNotNull(testEntity);
      assertTrue(entityManager.contains(testEntity));
      assertEquals(existingTestEntity.getId(), testEntity.getId().intValue());

      transactionManager.commit(transactionStatus);

      assertActualTransactionIsNotActive();

      return null;
    });
  }

  private TransactionStatus createTransaction(boolean readOnly) {
    DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    transactionDefinition.setReadOnly(readOnly);
    return transactionManager.getTransaction(transactionDefinition);
  }

  private static void assertReadWriteTransaction() {
    assertTrue(isSynchronizationActive());
    assertTrue(isActualTransactionActive());
    assertFalse(isCurrentTransactionReadOnly());
  }

  private static void assertReadOnlyMode() {
    assertTrue(isSynchronizationActive());
    assertFalse(isActualTransactionActive()); // means no transaction when @Transactional(readOnly=true) is used
  }

  private static void assertActualTransactionIsNotActive() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }

  private TestEntity createTestEntity() {
    TestEntity testEntity = new TestEntity("test entity");
    entityManager.persist(testEntity);
    return testEntity;
  }
}
