package ru.hh.nab.hibernate;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.hibernate.datasource.RoutingDataSource;
import ru.hh.nab.hibernate.model.TestEntity;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;
import ru.hh.nab.testbase.hibernate.NabHibernateTestBaseConfig;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.executeOn;

@ContextConfiguration(classes = {NabHibernateTestBaseConfig.class, NabHibernateCommonConfig.class,
  DataSourceSwitchingTest.DataSourceSwitchingTestConfig.class})
public class DataSourceSwitchingTest extends HibernateTestBase {
  @Inject
  private TransactionalScope transactionalScope;
  @Inject
  @Named("firstDataSourceSpy")
  private DataSource firstDataSourceSpy;
  @Inject
  @Named("secondDataSourceSpy")
  private DataSource secondDataSourceSpy;

  @Test
  public void testDsManageInsideTxScope() throws SQLException, ExecutionException, InterruptedException {
    Executor executor = Executors.newFixedThreadPool(1);
    CompletableFuture.supplyAsync(() -> {
        Supplier<TestEntity> supplier = () -> {
          Session currentSession = sessionFactory.getCurrentSession();
          return currentSession.find(TestEntity.class, 1);
        };
        TargetMethod<TestEntity> method = () -> executeOn("second", supplier);
        return transactionalScope.read(method);
    }, executor).get();
    //on init it calls 3 times before first query
    verify(firstDataSourceSpy, times(3)).getConnection();
    verify(secondDataSourceSpy, times(1)).getConnection();
  }

  @Test
  public void testTxScopeDoesntChangeDs() throws SQLException, ExecutionException, InterruptedException {
    Executor executor = Executors.newFixedThreadPool(1);
    CompletableFuture.supplyAsync(() -> {
        TargetMethod<TestEntity> method = () -> {
          Session currentSession = sessionFactory.getCurrentSession();
          return currentSession.find(TestEntity.class, 1);
        };
        Supplier<TestEntity> supplier = () -> {
          transactionalScope.read(method);
          return null;
        };
        return executeOn("second", supplier);
      }, executor).get();
    //on init it calls 3 times before first query
    verify(firstDataSourceSpy, times(3)).getConnection();
    verify(secondDataSourceSpy, times(1)).getConnection();
  }

  @Configuration
  static class DataSourceSwitchingTestConfig {
    static final String TEST_PACKAGE = "ru.hh.nab.hibernate.model.test";

    @Bean
    DataSourceFactory dataSourceFactory() {
      return new EmbeddedPostgresDataSourceFactory();
    }

    @Bean
    DataSource firstDataSourceSpy(DataSourceFactory dataSourceFactory) {
      return createDsSpy(dataSourceFactory, "first");
    }

    @Bean
    DataSource secondDataSourceSpy(DataSourceFactory dataSourceFactory) {
      return createDsSpy(dataSourceFactory, "second");
    }


    @Primary
    @Bean
    RoutingDataSource dataSource(DataSource firstDataSourceSpy, DataSource secondDataSourceSpy) {
      RoutingDataSource routingDataSource = new RoutingDataSource(firstDataSourceSpy);
      routingDataSource.addDataSource("two", secondDataSourceSpy);
      return routingDataSource;
    }

    @Bean
    MappingConfig mappingConfig() {
      MappingConfig mappingConfig = new MappingConfig(TestEntity.class);
      mappingConfig.addPackagesToScan(TEST_PACKAGE);
      return mappingConfig;
    }

    @Bean
    TransactionalScope transactionalScope() {
      return new TransactionalScope();
    }

    private static DataSource createDsSpy(DataSourceFactory dataSourceFactory, String key) {
      Properties properties = new Properties();
      properties.setProperty(key + ".pool.maximumPoolSize", "2");
      return spy(dataSourceFactory.create(key, false, new FileSettings(properties)));
    }
  }

  @FunctionalInterface
  interface TargetMethod<T> {
    T invoke();
  }

  static class TransactionalScope {

    @Transactional(readOnly = true)
    public <T> T read(TargetMethod<T> method) {
      return method.invoke();
    }

    @Transactional
    public <T> T write(TargetMethod<T> method) {
      return method.invoke();
    }
  }
}
