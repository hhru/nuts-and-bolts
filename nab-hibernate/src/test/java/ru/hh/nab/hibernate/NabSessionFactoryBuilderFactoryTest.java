package ru.hh.nab.hibernate;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSource;
import ru.hh.nab.hibernate.transaction.TransactionalScope;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NabSessionFactoryBuilderFactoryTest.TestContext.class)
public class NabSessionFactoryBuilderFactoryTest {

  private static Connection connection;

  @BeforeEach
  public void setUp() throws SQLException {
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);
    ResultSet rs = mock(ResultSet.class);
    when(rs.getMetaData()).thenReturn(metaData);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);
    connection = spy(Connection.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);

  }

  @Inject
  private TestService testService;

  @Test
  public void testConnectionClosedAfterStatement() throws SQLException {
    testService.method();
  }

  private static class TestService {
    private final SessionFactory sessionFactory;
    private final TransactionalScope transactionalScope;


    TestService(SessionFactory sessionFactory, TransactionalScope transactionalScope) {
      this.sessionFactory = sessionFactory;
      this.transactionalScope = transactionalScope;
    }

    @ExecuteOnDataSource(dataSourceType = DataSourceType.READONLY)
    public void method() throws SQLException {
      AtomicReference<Connection> ref = new AtomicReference<>();
      transactionalScope.read(() -> {
        try {
          Session currentSession = sessionFactory.getCurrentSession();
          ref.set(((SharedSessionContractImplementor) currentSession).getJdbcConnectionAccess().obtainConnection());
          verify(((SharedSessionContractImplementor) currentSession).getJdbcConnectionAccess().obtainConnection(), times(0)).close();
          currentSession.createNativeQuery("select 1 from dual").uniqueResult();
          verify(((SharedSessionContractImplementor) currentSession).getJdbcConnectionAccess().obtainConnection(), times(1)).close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });
      verify(ref.get(), times(1)).close();
    }
  }

  @Configuration
  @EnableTransactionManagement(order = 0)
  @EnableAspectJAutoProxy
  static class TestContext {
    @Bean
    DataSource dataSource() {
      DataSource hikariDataSource = new HikariDataSource() {
        @Override
        public Connection getConnection() throws SQLException {
          return connection;
        }
      };
      return hikariDataSource;
    }

    @Bean
    DataSourceContextTransactionManager transactionManager(SessionFactory sessionFactory, DataSource routingDataSource) {
      HibernateTransactionManager simpleTransactionManager = new HibernateTransactionManager(sessionFactory);
      simpleTransactionManager.setDataSource(routingDataSource);
      return new DataSourceContextTransactionManager(simpleTransactionManager);
    }

    @Bean
    NabSessionFactoryBean.ServiceSupplier<?> nabSessionFactoryBuilderServiceSupplier() {
      return new NabSessionFactoryBean.ServiceSupplier<NabSessionFactoryBuilderFactory.BuilderService>() {
        @Override
        public Class<NabSessionFactoryBuilderFactory.BuilderService> getClazz() {
          return NabSessionFactoryBuilderFactory.BuilderService.class;
        }

        @Override
        public NabSessionFactoryBuilderFactory.BuilderService get() {
          return new NabSessionFactoryBuilderFactory.BuilderService();
        }
      };
    }

    @Bean
    NabSessionFactoryBean sessionFactoryBean(DataSource dataSource, NabSessionFactoryBean.ServiceSupplier<Service> supplier) throws IOException {
      var props = new Properties();
      props.load(TestContext.class.getResourceAsStream("/hibernate-test.properties"));
      return new NabSessionFactoryBean(dataSource, props,
          new BootstrapServiceRegistryBuilder(), List.of(supplier), List.of()
      );
    }

    @Bean
    TransactionalScope transactionalScope() {
      return new TransactionalScope();
    }

    @Bean
    TestService testService(SessionFactory sessionFactory, TransactionalScope transactionalScope) {
      return new TestService(sessionFactory, transactionalScope);
    }
  }
}
