package ru.hh.nab.async;

import com.google.common.base.Function;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.glassfish.grizzly.http.server.Request;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.nab.NabModule;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.hibernate.Default;
import ru.hh.nab.hibernate.Transactional;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.testing.JerseyTest;

public class GuicyAsyncExecutorTest extends JerseyTest {
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
    return props;
  }

  @Override
  protected Module module() {
    return new NabModule() {
      @Override
      protected void configureApp() {
        bindDataSourceAndEntityManagerAccessor(TestEntity.class);
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

    final AtomicReference<TestEntity> result = new AtomicReference<TestEntity>();
    final CountDownLatch latch = new CountDownLatch(1);

    RequestScope.enter(mock(Request.class), new TimingsLogger("Test", "EmptyRequestId", Collections.<String, Long>emptyMap(), 1000L));

    ama.asyncWithTransferredRequestScope(new Callable<Integer>() {
      @Inject
      @Default
      EntityManager store;

      @Override
      @Transactional
      public Integer call() {
        TestEntity e = new TestEntity();
        e.name = "Foo";
        store.persist(e);
        return e.getId();
      }
    }).then(new Function<Integer, Async<TestEntity>>() {
      @Override
      public Async<TestEntity> apply(@Nullable final Integer id) {
        return ama.asyncWithTransferredRequestScope(new Callable<TestEntity>() {
          @Inject
          @Default
          EntityManager store;

          @Override
          @Transactional
          public TestEntity call() {
            return store.find(TestEntity.class, id);
          }
        });
      }
    }).run(new Callback<TestEntity>() {
      @Override
      public void call(TestEntity arg) throws Exception {
        result.set(arg);
        latch.countDown();
      }
    }, Callbacks.<Throwable>countDown(latch));
    RequestScope.leave();

    latch.await(10, TimeUnit.SECONDS);

    Assert.assertNotNull(result.get());
    Assert.assertEquals("Foo", result.get().getName());
  }

  @Test
  public void computationResumesInAnotherThread() throws InterruptedException {
    final GuicyAsyncExecutor ama = injector().getInstance(GuicyAsyncExecutor.class);

    final AtomicReference<TestEntity> result = new AtomicReference<TestEntity>();
    final CountDownLatch latch = new CountDownLatch(1);

    RequestScope.enter(mock(Request.class), new TimingsLogger("Test", "EmptyRequestId", Collections.<String, Long>emptyMap(), 1000L));

    ama.asyncWithTransferredRequestScope(new Callable<Integer>() {
      @Inject
      @Default
      EntityManager store;

      @Override
      @Transactional
      public Integer call() {
        GuicyAsyncExecutor.killThisThreadAfterExecution();
        TestEntity e = new TestEntity();
        e.name = "Foo";
        store.persist(e);
        return e.getId();
      }
    }).then(new Function<Integer, Async<TestEntity>>() {
      @Override
      public Async<TestEntity> apply(@Nullable final Integer id) {
        return ama.asyncWithTransferredRequestScope(new Callable<TestEntity>() {
          @Inject
          @Default
          EntityManager store;

          @Override
          @Transactional
          public TestEntity call() {
            GuicyAsyncExecutor.killThisThreadAfterExecution();
            return store.find(TestEntity.class, id);
          }
        });
      }
    }).run(new Callback<TestEntity>() {
      @Override
      public void call(TestEntity arg) throws Exception {
        result.set(arg);
        latch.countDown();
      }
    }, Callbacks.<Throwable>countDown(latch));
    RequestScope.leave();

    latch.await(10, TimeUnit.SECONDS);

    Assert.assertNotNull(result.get());
    Assert.assertEquals("Foo", result.get().getName());
  }
}
