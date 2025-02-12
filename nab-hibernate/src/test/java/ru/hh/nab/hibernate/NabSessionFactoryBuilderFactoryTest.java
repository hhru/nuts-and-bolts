package ru.hh.nab.hibernate;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.datasource.transaction.TransactionalScope;
import ru.hh.nab.hibernate.adapter.NabHibernateJpaVendorAdapter;
import ru.hh.nab.hibernate.adapter.NabHibernatePersistenceProvider;
import ru.hh.nab.hibernate.properties.HibernatePropertiesProvider;
import ru.hh.nab.hibernate.service.NabServiceContributor;
import ru.hh.nab.hibernate.service.ServiceSupplier;

@SpringBootTest(classes = NabSessionFactoryBuilderFactoryTest.TestContext.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class NabSessionFactoryBuilderFactoryTest {

  private static final Connection connection = spy(Connection.class);

  @Inject
  private Session session;
  @Inject
  private TransactionalScope transactionalScope;

  @BeforeEach
  public void setUp() throws SQLException {
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);
    ResultSet rs = mock(ResultSet.class);
    when(rs.getMetaData()).thenReturn(metaData);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(ps.executeQuery()).thenReturn(rs);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
  }

  @AfterEach
  public void tearDown() {
    reset(connection);
  }

  @Test
  public void testConnectionClosedAfterStatement() throws SQLException {
    transactionalScope.read(() -> {
      try {
        verify(connection, times(0)).close();
        session.createNativeQuery("select 1 from dual").uniqueResult();
        verify(connection, times(1)).close();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    verify(connection, times(1)).close();
  }

  @Test
  public void testConnectionClosedAfterStatementAndAfterTransaction() throws SQLException {
    transactionalScope.write(() -> {
      try {
        verify(connection, times(0)).close();
        session.createNativeQuery("select 1 from dual").uniqueResult();
        verify(connection, times(1)).close();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    verify(connection, times(2)).close();
  }

  @Configuration
  @EnableTransactionManagement
  @Import({
      JpaConfig.class,
      NabHibernateJpaVendorAdapter.class,
      NabHibernatePersistenceProvider.class,
      NabServiceContributor.class,
  })
  static class TestContext {
    @Bean
    TransactionalScope transactionalScope() {
      return new TransactionalScope();
    }

    @Primary
    @Bean
    DataSourceContextTransactionManager primaryTransactionManager(EntityManagerFactory entityManagerFactory) {
      JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
      return new DataSourceContextTransactionManager(jpaTransactionManager, null);
    }

    @Bean
    Session session(EntityManagerFactory entityManagerFactory) {
      return (Session) entityManagerFactory.createEntityManager();
    }

    @Bean
    DataSource dataSource() throws SQLException {
      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenReturn(connection);
      return dataSource;
    }

    @Bean
    ServiceSupplier<NabSessionFactoryBuilderFactory.BuilderService> nabSessionFactoryBuilderServiceSupplier() {
      return new ServiceSupplier<>() {
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
    HibernatePropertiesProvider hibernatePropertiesProvider() throws IOException {
      Properties hibernateProperties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("hibernate-test.properties"));
      return new HibernatePropertiesProvider(hibernateProperties);
    }
  }

  @Configuration
  @EnableConfigurationProperties(JpaProperties.class)
  static class JpaConfig extends JpaBaseConfiguration {
    private final AbstractJpaVendorAdapter jpaVendorAdapter;
    private final HibernatePropertiesProvider hibernatePropertiesProvider;

    protected JpaConfig(
        DataSource dataSource,
        JpaProperties properties,
        ObjectProvider<JtaTransactionManager> jtaTransactionManager,
        AbstractJpaVendorAdapter jpaVendorAdapter,
        HibernatePropertiesProvider hibernatePropertiesProvider
    ) {
      super(dataSource, properties, jtaTransactionManager);
      this.jpaVendorAdapter = jpaVendorAdapter;
      this.hibernatePropertiesProvider = hibernatePropertiesProvider;
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
      return jpaVendorAdapter;
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
      return new HashMap<>(PropertiesUtils.getAsMap(hibernatePropertiesProvider.get()));
    }
  }
}
