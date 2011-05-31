package ru.hh.nab.hibernate;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import java.io.IOException;
import java.util.Properties;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.Launcher;
import ru.hh.nab.ModelAccess;
import ru.hh.nab.ModelAction;
import ru.hh.nab.NabModule;
import ru.hh.nab.Settings;

public class PersistenceTest {
  @Entity(name = "TestEntity")
  @Table(name = "test")
  public static class TestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
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
    @Inject
    @Default
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

    Launcher.Instance inst = Launcher.testMode(Stage.DEVELOPMENT, new NabModule() {
      @Override
      protected void configureApp() {
        bindDataSourceAndEntityManagerAccessor(TestEntity.class);

        bind(EntityManagerWrapper.class).in(Scopes.SINGLETON);
      }
    }, props, new Properties());

//    Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new NabModule() {
//      @Override
//      protected void configureApp() {
//        bindDataSourceAndEntityManagerAccessor(TestEntity.class);
//
//        bind(EntityManagerWrapper.class).in(Scopes.SINGLETON);
//      }
//
//      @Provides
//      @Singleton
//      Settings settings() {
//        Properties props = new Properties();
//        props.put("concurrencyLevel", "1");
//        props.put("port", "0");
//
//        props.put("default-db.hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
//        props.put("default-db.hibernate.hbm2ddl.auto", "update");
//        props.put("default-db.hibernate.format_sql", "true");
//
//        props.put("default-db.c3p0.jdbcUrl", "jdbc:hsqldb:mem:" + getClass().getName());
//        props.put("default-db.c3p0.driverClass", "org.hsqldb.jdbcDriver");
//        props.put("default-db.c3p0.user", "sa");
//        props.put("default-db.c3p0.password", "");
//
//        return new Settings(props);
//      }
//    });

    EntityManagerWrapper em = inst.injector.getInstance(EntityManagerWrapper.class);
    ModelAccess ma = inst.injector.getInstance(Key.get(ModelAccess.class, Default.class));

    TestEntity entity = new TestEntity();
    entity.setName("42");
    int id = em.persist(entity);

    entity = em.get(id);
    Assert.assertEquals("42", entity.getName());

    ma.perform(new ModelAction<TestEntity>() {
      @Override
      public TestEntity perform(EntityManager store) {
        TypedQuery<TestEntity> q = store.createQuery("from TestEntity where name = :name", TestEntity.class);
        return q.setParameter("name", "42").getSingleResult();
      }
    });
  }
}

