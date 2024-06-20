package ru.hh.nab.hibernate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.SessionImpl;
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
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.hibernate.properties.HibernatePropertiesProvider;
import ru.hh.nab.jdbc.DataSourceType;
import ru.hh.nab.jdbc.annotation.ExecuteOnDataSource;
import ru.hh.nab.jdbc.common.DataSourcePropertiesStorage;
import ru.hh.nab.jdbc.transaction.TransactionalScope;
import ru.hh.nab.metrics.StatsDSender;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NabSessionFactoryBuilderFactoryTest.TestContext.class)
public class NabSessionFactoryBuilderFactoryTest {

  @Inject
  private TestService testService;

  @Test
  public void testConnectionClosedAfterStatement() throws SQLException {
    testService.method();
  }

  private static class TestService {
    private final EntityManager entityManager;
    private final TransactionalScope transactionalScope;

    TestService(EntityManager entityManager, TransactionalScope transactionalScope) {
      this.entityManager = entityManager;
      this.transactionalScope = transactionalScope;
    }

    @ExecuteOnDataSource(dataSourceType = DataSourceType.READONLY)
    public void method() throws SQLException {
      AtomicReference<Connection> ref = new AtomicReference<>();
      transactionalScope.read(() -> {
        try {
          Session currentSession = entityManager.unwrap(SessionImpl.class);
          ref.set(((SharedSessionContractImplementor) currentSession).getJdbcConnectionAccess().obtainConnection());
          verify(((SharedSessionContractImplementor) currentSession).getJdbcConnectionAccess().obtainConnection(), times(0)).close();
          entityManager.createNativeQuery("select 1 from dual").getResultList();
          verify(((SharedSessionContractImplementor) currentSession).getJdbcConnectionAccess().obtainConnection(), times(1)).close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });
      verify(ref.get(), times(1)).close();
    }
  }

  @Configuration
  @Import({
      NabHibernateCommonConfig.class,
      TestService.class,
  })
  static class TestContext {

    @Bean
    @Named(SERVICE_NAME)
    String serviceName() {
      return "test-service";
    }

    @Bean
    StatsDSender statsDSender() {
      return mock(StatsDSender.class);
    }

    @Bean
    DataSource dataSource() throws SQLException {
      ResultSetMetaData metaData = mock(ResultSetMetaData.class);

      ResultSet rs = mock(ResultSet.class);
      when(rs.getMetaData()).thenReturn(metaData);

      PreparedStatement ps = mock(PreparedStatement.class);
      when(ps.executeQuery()).thenReturn(rs);

      Connection connection = spy(Connection.class);
      when(connection.prepareStatement(anyString())).thenReturn(ps);

      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenReturn(connection);

      DataSourcePropertiesStorage.registerPropertiesFor(
          DataSourceType.READONLY,
          new DataSourcePropertiesStorage.DataSourceProperties(false, null)
      );
      return dataSource;
    }

    @Bean
    HibernatePropertiesProvider hibernatePropertiesProvider() throws IOException {
      Properties hibernateProperties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("hibernate-test.properties"));
      return new HibernatePropertiesProvider(hibernateProperties);
    }
  }
}
