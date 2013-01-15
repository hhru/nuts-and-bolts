package ru.hh.nab.hibernate;

import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import java.io.IOException;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import org.junit.Assert;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import ru.hh.nab.Launcher;
import ru.hh.nab.ModelAccess;
import ru.hh.nab.ModelAction;
import ru.hh.nab.NabModule;

public class PersistenceTest {
  @Test
  public void test() throws IOException {
    Properties props = new Properties();
    props.put("concurrencyLevel", "1");
    props.put("port", "0");

    props.put("default-db.hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    props.put("default-db.hibernate.hbm2ddl.auto", "update");
    props.put("default-db.hibernate.format_sql", "true");

    props.put("default-db.c3p0.jdbcUrl", "jdbc:hsqldb:mem:" + getClass().getName());
    props.put("default-db.c3p0.driverClass", "org.hsqldb.jdbcDriver");
    props.put("default-db.c3p0.user", "sa");
    props.put("default-db.c3p0.password", "");

    Launcher.Instance inst = Launcher.testMode(
      Stage.DEVELOPMENT,
      new NabModule() {
        @Override
        protected void configureApp() {
          bindDataSourceAndEntityManagerAccessor(TestEntity.class);

          bind(EntityManagerWrapper.class).in(Scopes.SINGLETON);
        }
      }, props, new Properties(), new Properties());

// Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new NabModule() {
// @Override
// protected void configureApp() {
// bindDataSourceAndEntityManagerAccessor(TestEntity.class);
//
// bind(EntityManagerWrapper.class).in(Scopes.SINGLETON);
// }
//
// @Provides
// @Singleton
// Settings settings() {
// Properties props = new Properties();
// props.put("concurrencyLevel", "1");
// props.put("port", "0");
//
// props.put("default-db.hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
// props.put("default-db.hibernate.hbm2ddl.auto", "update");
// props.put("default-db.hibernate.format_sql", "true");
//
// props.put("default-db.c3p0.jdbcUrl", "jdbc:hsqldb:mem:" + getClass().getName());
// props.put("default-db.c3p0.driverClass", "org.hsqldb.jdbcDriver");
// props.put("default-db.c3p0.user", "sa");
// props.put("default-db.c3p0.password", "");
//
// return new Settings(props);
// }
// });

    EntityManagerWrapper em = inst.injector.getInstance(EntityManagerWrapper.class);
    ModelAccess ma = inst.injector.getInstance(Key.get(ModelAccess.class, Default.class));

    TestEntity entity = new TestEntity();
    entity.setName("42");
    int id = em.persist(entity);

    entity = em.get(id);
    Assert.assertEquals("42", entity.getName());

    ma.perform(
      new ModelAction<TestEntity>() {
        @Override
        public TestEntity perform(EntityManager store) {
          TypedQuery<TestEntity> q = store.createQuery("from TestEntity where name = :name", TestEntity.class);
          return q.setParameter("name", "42").getSingleResult();
        }
      });
  }

  @Test
  public void postCommitActions() throws IOException {
    Properties props = new Properties();
    props.put("concurrencyLevel", "1");
    props.put("port", "0");

    props.put("default-db.hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    props.put("default-db.hibernate.hbm2ddl.auto", "update");
    props.put("default-db.hibernate.format_sql", "true");

    props.put("default-db.c3p0.jdbcUrl", "jdbc:hsqldb:mem:" + getClass().getName());
    props.put("default-db.c3p0.driverClass", "org.hsqldb.jdbcDriver");
    props.put("default-db.c3p0.user", "sa");
    props.put("default-db.c3p0.password", "");

    Launcher.Instance inst = Launcher.testMode(
      Stage.DEVELOPMENT,
      new NabModule() {
        @Override
        protected void configureApp() {
          bindDataSourceAndEntityManagerAccessor(TestEntity.class);
          bind(TestService.class).in(Scopes.SINGLETON);
          bind(TestHook.class);
        }
      }, props, new Properties(), new Properties());

    TestService service = inst.injector.getInstance(TestService.class);
    TestHook hook = inst.injector.getInstance(TestHook.class);
    service.txMethod(hook);
    assertTrue(hook.called());

    hook = inst.injector.getInstance(TestHook.class);
    TestHook hookOptional = inst.injector.getInstance(TestHook.class);
    service.txOptionalMethod(hook, hookOptional);
    assertTrue(hook.called());
    assertTrue(hookOptional.called());

    hook = inst.injector.getInstance(TestHook.class);
    hookOptional = inst.injector.getInstance(TestHook.class);
    try {
      service.txOptionalMethodWithError1(hook, hookOptional);
      fail();
    } catch (IllegalArgumentException expected) { }
    assertTrue(hook.called());
    assertFalse(hookOptional.called());

    hook = inst.injector.getInstance(TestHook.class);
    hookOptional = inst.injector.getInstance(TestHook.class);
    try {
      service.txOptionalMethodWithError2(hook, hookOptional);
      fail();
    } catch (PersistenceException expected) { }
    assertFalse(hook.called());
    assertFalse(hookOptional.called());

    hook = inst.injector.getInstance(TestHook.class);
    try {
      service.txMethodWithError(hook);
      fail();
    } catch (PersistenceException expected) { }
    assertFalse(hook.called());
  }

  @Entity(name = "TestEntity")
  @Table(name = "test")
  public static class TestEntity {
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Id
    private Integer id;

    @Basic(optional = false)
    private String name;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class EntityManagerWrapper {
    @Default
    @Inject
    Provider<EntityManager> em;

    @Transactional
    public int persist(TestEntity e) {
      em.get().persist(e);
      return e.id;
    }

    @Transactional(readOnly = true)
    public TestEntity get(int id) {
      return em.get().find(TestEntity.class, id);
    }
  }

  public static class TestService {
    private final Provider<EntityManager> em;
    private final Provider<PostCommitHooks> postCommitActions;

    @Inject
    public TestService(@Default
        Provider<EntityManager> em, @Default
        Provider<PostCommitHooks> postCommitActions) {
      this.em = em;
      this.postCommitActions = postCommitActions;
    }

    @Transactional
    public void txMethod(TestHook hook) {
      final TestEntity e = new TestEntity();
      e.setName("foo");
      postCommitActions.get().addHook(hook);
      em.get().persist(e);
    }

    @Transactional
    public void txMethodWithError(TestHook hook) {
      final TestEntity e = new TestEntity();
      postCommitActions.get().addHook(hook);
      em.get().persist(e);
    }

    @Transactional(optional = true)
    public void txOptionalMethod(TestHook hook2, TestHook hook3) {
      txMethod(hook2);
      postCommitActions.get().addHook(hook3);
    }

    @Transactional(optional = true)
    public void txOptionalMethodWithError1(TestHook hook, TestHook hookOptional) throws PersistenceException {
      txMethod(hook);
      postCommitActions.get().addHook(hookOptional);
      throw new IllegalArgumentException("your argument is invalid");
    }

    @Transactional(optional = true)
    public void txOptionalMethodWithError2(TestHook hook, TestHook hookOptional) {
      postCommitActions.get().addHook(hook);
      txMethodWithError(hook);
    }
  }

  public static class TestHook implements Runnable {
    private final Provider<EntityManager> em;
    private boolean called;

    @Inject
    public TestHook(@Default
        Provider<EntityManager> em) {
      this.em = em;
    }

    @Override
    public void run() {
      try {
        em.get();
        fail();
      } catch (ProvisionException notInTxt) { }
      called = true;
    }

    public boolean called() {
      return called;
    }
  }
}
