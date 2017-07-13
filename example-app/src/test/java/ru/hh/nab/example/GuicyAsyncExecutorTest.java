package ru.hh.nab.example;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.NabModule;
import ru.hh.nab.hibernate.Default;
import ru.hh.nab.hibernate.Transactional;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.testing.JerseyTest;
import javax.inject.Inject;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Provider;

import static org.mockito.Mockito.mock;
import ru.hh.nab.async.Callbacks;
import ru.hh.nab.async.GuicyAsyncExecutor;

public class GuicyAsyncExecutorTest extends JerseyTest {
  @Entity(name = "TestEntity")
  @Table(name = "test")
  private static class TestEntity {
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

  static class TestService {

    private final Provider<EntityManager> em;

    @Inject
    TestService(@Default Provider<EntityManager> em) {
      this.em = em;
    }

    @Transactional
    public void persist(TestEntity testEntity) {
      em.get().persist(testEntity);
    }

    @Transactional(readOnly = true)
    public TestEntity find(Integer id) {
      return em.get().find(TestEntity.class, id);
    }
  }

  @Override
  protected Properties settings() {
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

    props.put("default-db.monitoring.sendStats", "false");
    props.put("default-db.monitoring.longConnectionUsageMs", "3000");
    return props;
  }

  @Override
  protected Module module() {
    return new NabModule() {
      @Override
      protected void configureApp() {
        bind(String.class).annotatedWith(Names.named("serviceName")).toInstance("serviceName");
        bindDataSourceAndEntityManagerAccessor(TestEntity.class);
        bind(TestService.class).in(Scopes.SINGLETON);
      }

      protected
      @Provides
      GuicyAsyncExecutor asyncModelAccessor(Injector inj) {
        return new GuicyAsyncExecutor(inj, "test", 4);
      }
    };
  }

  @Test
  public void basicOperation() throws InterruptedException {
    final GuicyAsyncExecutor ama = injector().getInstance(GuicyAsyncExecutor.class);

    final AtomicReference<TestEntity> result = new AtomicReference<>();
    final AtomicReference<Integer> resultId = new AtomicReference<>();
    final CountDownLatch syncLatch = new CountDownLatch(1);
    final CountDownLatch finalLatch = new CountDownLatch(1);

    RequestScope.enter(mock(HttpServletRequest.class), mock(HttpServletResponse.class));

    ama.asyncWithTransferredRequestScope(new Callable<Integer>() {

      @Inject
      private TestService testService;

      @Override
      public Integer call() {
        TestEntity e = new TestEntity();
        e.setName("Foo");
        testService.persist(e);
        return e.getId();
      }
    }).run(id -> {
      resultId.set(id);
      syncLatch.countDown();
    }, Callbacks.<Throwable>countDown(finalLatch));


    ama.asyncWithTransferredRequestScope(new Callable<TestEntity>() {
      @Inject
      private TestService testService;

      @Override
      public TestEntity call() {
        try {
          syncLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        return testService.find(resultId.get());
      }
    }).run(resultEntity -> {
      result.set(resultEntity);
      finalLatch.countDown();
    }, Callbacks.<Throwable>countDown(finalLatch));

    RequestScope.leave();

    finalLatch.await(10, TimeUnit.SECONDS);

    Assert.assertNotNull(result.get());
    Assert.assertEquals("Foo", result.get().getName());
  }
}
