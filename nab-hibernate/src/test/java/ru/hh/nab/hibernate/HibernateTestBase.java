package ru.hh.nab.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.BeforeClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public abstract class HibernateTestBase {

  private static AnnotationConfigApplicationContext context;
  protected static SessionFactory sessionFactory;
  protected static PlatformTransactionManager transactionManager;
  protected static int existingTestEntityId;

  private static TransactionStatus transactionStatus;

  @BeforeClass
  public static void setUpTestBaseClass() {
    context = new AnnotationConfigApplicationContext();

    context.registerBean("mappingConfig", MappingConfig.class, () -> {
      MappingConfig mappingConfig = new MappingConfig();
      mappingConfig.addMapping(TestEntity.class);
      return mappingConfig;
    });
    context.register(HibernateTestConfig.class, HibernateCommonConfig.class);
    context.refresh();

    transactionManager = context.getBean(PlatformTransactionManager.class);
    sessionFactory = context.getBean(SessionFactory.class);

    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      TestEntity testEntity = createTestEntity(session);
      session.getTransaction().commit();
      existingTestEntityId = testEntity.getId();
    }
  }

  protected void startTransaction() {
    DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionStatus = transactionManager.getTransaction(transactionDefinition);
  }

  protected static void rollBackTransaction() {
    transactionManager.rollback(transactionStatus);
    transactionStatus = null;
  }

  protected static TestEntity createTestEntity() {
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

  protected static Session getCurrentSession() {
    return sessionFactory.getCurrentSession();
  }

  protected static <T> T getBean(Class<T> aClass) {
    return context.getBean(aClass);
  }
}
