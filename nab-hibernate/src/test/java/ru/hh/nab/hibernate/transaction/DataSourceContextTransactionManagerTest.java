package ru.hh.nab.hibernate.transaction;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;

import ru.hh.nab.hibernate.HibernateTestBase;
import ru.hh.nab.hibernate.TestEntity;
import ru.hh.nab.datasource.DataSourceType;

public class DataSourceContextTransactionManagerTest extends HibernateTestBase {
  private int existingTestEntityId;

  @Before
  public void setUpTestBaseClass() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      TestEntity testEntity = createTestEntity(session);
      session.getTransaction().commit();
      existingTestEntityId = testEntity.getId();
    }
  }

  @Test(expected = HibernateException.class)
  public void noDefaultSynchronizationInTests() {
    getCurrentSession();
  }

  @Test
  public void transactionOnMaster() {
    TransactionStatus transactionStatus = createTransaction(false);

    assertReadWriteTransaction();

    TestEntity newTestEntity = createTestEntity();
    TestEntity newTestEntityFromDb = getCurrentSession().get(TestEntity.class, newTestEntity.getId());
    assertNotNull(newTestEntityFromDb);

    transactionManager.rollback(transactionStatus);
    assertHibernateIsNotInitialized();

    transactionStatus = createTransaction(false);
    assertNull(getCurrentSession().get(TestEntity.class, newTestEntity.getId()));
    transactionManager.rollback(transactionStatus);
  }

  @Test
  public void readOnlyTransactionOnMasterShouldOnlyInitializeHibernate() {
    TransactionStatus readOnlyTransactionStatus = createTransaction(true);
    assertReadOnlyMode();

    Session session = getCurrentSession();
    assertTrue(session.getStatistics().getEntityKeys().isEmpty());

    TestEntity testEntityFromDb = session.get(TestEntity.class, existingTestEntityId);
    assertNotNull(testEntityFromDb);
    assertTrue(session.contains(testEntityFromDb));
    assertEquals(existingTestEntityId, testEntityFromDb.getId().intValue());

    transactionManager.commit(readOnlyTransactionStatus);

    assertHibernateIsNotInitialized();
  }

  @Test
  public void testMasterWithReadOnlyFlagInsideMaster() {
    TransactionStatus outerReadWriteTransactionStatus = createTransaction(false);

    TransactionStatus innerReadOnlyTransactionStatus = createTransaction(true);

    assertReadWriteTransaction();

    Session session = getCurrentSession();
    assertSessionIsEmpty(session);
    assertNotNull(session.get(TestEntity.class, existingTestEntityId));

    transactionManager.commit(innerReadOnlyTransactionStatus);

    assertReadWriteTransaction();

    TestEntity newTestEntity = createTestEntity();
    assertNotNull(session.get(TestEntity.class, newTestEntity.getId()));

    transactionManager.rollback(outerReadWriteTransactionStatus);

    assertHibernateIsNotInitialized();
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

    assertHibernateIsNotInitialized();
  }

  @Test
  public void testDataSource() {
    testDataSource(DataSourceType.READONLY);
  }

  @Test
  public void testSlowReplica() {
    testDataSource(DataSourceType.SLOW);
  }

  private void testDataSource(DataSourceType dataSourceType) {
    assertHibernateIsNotInitialized();

    DataSourceContextUnsafe.executeOn(dataSourceType, () -> {
      TransactionStatus transactionStatus = createTransaction(false);

      assertReadOnlyMode();

      Session session = getCurrentSession();
      assertSessionIsEmpty(session);

      TestEntity testEntity = session.get(TestEntity.class, existingTestEntityId);
      assertNotNull(testEntity);
      assertTrue(session.contains(testEntity));
      assertEquals(existingTestEntityId, testEntity.getId().intValue());

      transactionManager.commit(transactionStatus);

      assertHibernateIsNotInitialized();

      return null;
    });
  }

  private static void assertSessionIsEmpty(Session session) {
    assertTrue(session.getStatistics().getEntityKeys().isEmpty());
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

  private static void assertHibernateIsNotInitialized() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }

  private TestEntity createTestEntity() {
    Session session = getCurrentSession();
    TestEntity testEntity = createTestEntity(session);
    session.flush();
    session.clear();
    return testEntity;
  }

  private static TestEntity createTestEntity(Session session) {
    TestEntity testEntity = new TestEntity("test entity");
    session.save(testEntity);
    return testEntity;
  }
}
