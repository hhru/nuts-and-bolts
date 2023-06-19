package ru.hh.nab.hibernate;

import com.codahale.metrics.health.HealthCheck;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.hibernate.Session;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.datasource.DataSourceFactory;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_ENABLED;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.ROUTING_SECONDARY_DATASOURCE;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSource;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.healthcheck.UnhealthyDataSourceException;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.hibernate.datasource.RoutingDataSource;
import ru.hh.nab.hibernate.datasource.RoutingDataSourceFactory;
import ru.hh.nab.hibernate.model.TestEntity;
import static ru.hh.nab.hibernate.transaction.DataSourceContext.onDataSource;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;
import ru.hh.nab.testbase.hibernate.NabHibernateTestBaseConfig;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

@ContextConfiguration(
    classes = {
        NabHibernateTestBaseConfig.class,
        NabHibernateCommonConfig.class,
        DataSourceSwitchingTest.DataSourceSwitchingTestConfig.class
    }
)
public class DataSourceSwitchingTest extends HibernateTestBase {
  @Inject
  private TransactionalScope transactionalScope;
  @Inject
  @Named("firstDataSourceSpy")
  private DataSource firstDataSourceSpy;
  @Inject
  @Named("secondDataSourceSpy")
  private DataSource secondDataSourceSpy;
  @Inject
  @Named("thirdDataSourceSpy")
  private DataSource thirdDataSourceSpy;
  @Inject
  @Named("fourthDataSourceSpy")
  private DataSource fourthDataSourceSpy;

  @BeforeEach
  public void setUp() {
    reset(firstDataSourceSpy);
    reset(secondDataSourceSpy);
    reset(thirdDataSourceSpy);
    reset(fourthDataSourceSpy);
  }

  @Test
  public void testDsManageInsideTxScope() throws Exception {
    Executor executor = Executors.newFixedThreadPool(1);
    CompletableFuture.supplyAsync(() -> {
      Supplier<TestEntity> supplier = () -> {
        Session currentSession = sessionFactory.getCurrentSession();
        return currentSession.find(TestEntity.class, 1);
      };
      TargetMethod<TestEntity> method = () -> onDataSource("second", supplier);
      return transactionalScope.read(method);
    }, executor).get();
    verify(firstDataSourceSpy, never()).getConnection();
    verify(secondDataSourceSpy, times(1)).getConnection();
    verify(thirdDataSourceSpy, never()).getConnection();
    verify(fourthDataSourceSpy, never()).getConnection();
  }

  @Test
  public void testTxScopeDoesntChangeDs() throws Exception {
    Executor executor = Executors.newFixedThreadPool(1);
    CompletableFuture.supplyAsync(() -> {
      TargetMethod<TestEntity> method = () -> {
        Session currentSession = sessionFactory.getCurrentSession();
        return currentSession.find(TestEntity.class, 1);
      };
      Supplier<TestEntity> supplier = () -> transactionalScope.read(method);
      return onDataSource("second", supplier);
    }, executor).get();
    verify(firstDataSourceSpy, never()).getConnection();
    verify(secondDataSourceSpy, times(1)).getConnection();
    verify(thirdDataSourceSpy, never()).getConnection();
    verify(fourthDataSourceSpy, never()).getConnection();
  }

  @Test
  public void testSwitchingIfSecondaryDataSourceIsNotPresent() throws Exception {
    doThrow(UnhealthyDataSourceException.class).when(thirdDataSourceSpy).getConnection();
    TargetMethod<TestEntity> method = () -> {
      Session currentSession = sessionFactory.getCurrentSession();
      return currentSession.find(TestEntity.class, 1);
    };
    PersistenceException ex = assertThrows(
        PersistenceException.class,
        () -> onDataSource("third", () -> transactionalScope.read(method))
    );
    assertTrue(ex.getCause() instanceof UnhealthyDataSourceException);
    verify(firstDataSourceSpy, never()).getConnection();
    verify(secondDataSourceSpy, never()).getConnection();
    verify(thirdDataSourceSpy, times(1)).getConnection();
    verify(fourthDataSourceSpy, never()).getConnection();
  }

  @Test
  public void testSwitchingIfSecondaryDataSourceIsPresent() throws Exception {
    doThrow(UnhealthyDataSourceException.class).when(fourthDataSourceSpy).getConnection();
    TargetMethod<TestEntity> method = () -> {
      Session currentSession = sessionFactory.getCurrentSession();
      return currentSession.find(TestEntity.class, 1);
    };
    onDataSource("fourth", () -> transactionalScope.read(method));

    verify(firstDataSourceSpy, never()).getConnection();
    verify(secondDataSourceSpy, times(1)).getConnection();
    verify(thirdDataSourceSpy, never()).getConnection();
    verify(fourthDataSourceSpy, never()).getConnection();
  }

  @Configuration
  static class DataSourceSwitchingTestConfig {
    static final String TEST_PACKAGE = "ru.hh.nab.hibernate.model.test";
    static final String SERVICE_NAME_VALUE = "test-service";

    @Bean
    StatsDSender statsDSender() {
      return mock(StatsDSender.class);
    }

    @Bean
    DataSourceFactory dataSourceFactory(StatsDSender statsDSender) {
      return new EmbeddedPostgresDataSourceFactory(
          new NabMetricsTrackerFactoryProvider(SERVICE_NAME_VALUE, statsDSender),
          new HealthCheckHikariDataSourceFactory(SERVICE_NAME_VALUE, statsDSender) {
            @Override
            public HikariDataSource create(HikariConfig hikariConfig) {
              return spy(super.create(hikariConfig));
            }
          }
      );
    }

    @Bean
    RoutingDataSourceFactory routingDataSourceFactory() {
      return new RoutingDataSourceFactory(null, null);
    }

    @Bean
    DataSource firstDataSourceSpy(DataSourceFactory dataSourceFactory) {
      String dataSourceName = "first";
      Properties properties = createProperties(dataSourceName);
      return createDsSpy(dataSourceFactory, dataSourceName, properties);
    }

    @Bean
    DataSource secondDataSourceSpy(DataSourceFactory dataSourceFactory) {
      String dataSourceName = "second";
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      return createDsSpy(dataSourceFactory, dataSourceName, properties);
    }

    @Bean
    DataSource thirdDataSourceSpy(DataSourceFactory dataSourceFactory) {
      String dataSourceName = "third";
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      return createDsSpy(dataSourceFactory, dataSourceName, properties);
    }

    @Bean
    DataSource fourthDataSourceSpy(DataSourceFactory dataSourceFactory) {
      String dataSourceName = "fourth";
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      properties.setProperty(dataSourceName + "." + ROUTING_SECONDARY_DATASOURCE, "second");
      return createDsSpy(dataSourceFactory, dataSourceName, properties);
    }

    @Primary
    @Bean
    RoutingDataSource dataSource(
        RoutingDataSourceFactory routingDataSourceFactory,
        DataSource firstDataSourceSpy,
        DataSource secondDataSourceSpy,
        DataSource thirdDataSourceSpy,
        DataSource fourthDataSourceSpy
    ) {
      RoutingDataSource routingDataSource = routingDataSourceFactory.create(firstDataSourceSpy);
      routingDataSource.addDataSource("second", secondDataSourceSpy);
      routingDataSource.addDataSource("third", thirdDataSourceSpy);
      routingDataSource.addDataSource("fourth", fourthDataSourceSpy);
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

    private static Properties createProperties(String dataSourceName) {
      Properties properties = new Properties();
      properties.setProperty(dataSourceName + ".pool.maximumPoolSize", "2");
      return properties;
    }

    private static DataSource createDsSpy(DataSourceFactory dataSourceFactory, String dataSourceName, Properties properties) {
      DataSource dataSource = spy(dataSourceFactory.create(dataSourceName, false, new FileSettings(properties)));
      try {
        var healthCheckHikariDataSource = dataSource.unwrap(HealthCheckHikariDataSource.class);
        HealthCheckHikariDataSource.AsyncHealthCheckDecorator failedHealthCheck = mock(HealthCheckHikariDataSource.AsyncHealthCheckDecorator.class);
        when(failedHealthCheck.check()).thenReturn(HealthCheck.Result.unhealthy("Data source is unhealthy"));
        when(healthCheckHikariDataSource.getHealthCheck()).thenReturn(failedHealthCheck);
      } catch (SQLException e) {
        // empty
      }
      return dataSource;
    }

    @Named(SERVICE_NAME)
    @Bean(SERVICE_NAME)
    String serviceName() {
      return SERVICE_NAME_VALUE;
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
