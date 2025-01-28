package ru.hh.nab.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Properties;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import ru.hh.nab.jpa.model.PackageNotScanEntity;
import ru.hh.nab.jpa.model.TestEntity;
import ru.hh.nab.jpa.model.test.PackageScanEntity;

@SpringBootTest(classes = MappingConfigTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class MappingConfigTest {

  private static final String TEST_PACKAGE = "ru.hh.nab.jpa.model.test";

  @Inject
  private EntityManager entityManager;

  @Test
  public void persistenceProviderShouldHaveMappedEntities() {
    assertTrue(metamodelContainsEntity(entityManager, TestEntity.class));
    assertTrue(metamodelContainsEntity(entityManager, PackageScanEntity.class));
    assertFalse(metamodelContainsEntity(entityManager, PackageNotScanEntity.class));
  }

  private boolean metamodelContainsEntity(EntityManager entityManager, Class<?> cls) {
    return entityManager
        .getEntityManagerFactory()
        .getMetamodel()
        .getEntities()
        .stream()
        .anyMatch(e -> e.getJavaType().equals(cls));
  }

  @Configuration
  // TODO: TestBeanDefinitionRegistryPostProcessor - это костыль, который нужен для того, чтобы выпилить из контекста бины из
  //  NabDataSourceCommonConfig (NabDataSourceCommonConfig явно импортится в NabJpaCommonConfig). После добавления
  //  data-source-spring-boot-starter'а этот костыль больше будет не нужен, так как для data-source и jpa будут добавлены отдельные
  //  автоконфигурации. Таким образом при выполнении задачи https://jira.hh.ru/browse/PORTFOLIO-38404 нужно удалить класс
  //  TestBeanDefinitionRegistryPostProcessor и поправить импорт на @ImportAutoConfiguration(JpaAutoConfiguration.class)
  @Import({
      TestBeanDefinitionRegistryPostProcessor.class,
      NabJpaCommonConfig.class
  })
  public static class TestConfiguration {

    @Bean
    public MappingConfig mappingConfig() {
      MappingConfig mappingConfig = new MappingConfig(TestEntity.class);
      mappingConfig.addPackagesToScan(TEST_PACKAGE);
      return mappingConfig;
    }

    @Bean
    public DataSource dataSource() {
      return mock(DataSource.class);
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
      return new HibernateJpaVendorAdapter();
    }

    @Bean
    public JpaPropertiesProvider jpaPropertiesProvider() {
      return () -> {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        return properties;
      };
    }
  }

  private static class TestBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      String dataSourcePackage = "ru.hh.nab.datasource";
      Arrays.stream(registry.getBeanDefinitionNames())
          .filter(name -> {
            BeanDefinition beanDefinition = registry.getBeanDefinition(name);
            return name.startsWith(dataSourcePackage) ||
                beanDefinition.getFactoryBeanName() != null && beanDefinition.getFactoryBeanName().startsWith(dataSourcePackage);
          })
          .forEach(registry::removeBeanDefinition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }
  }
}
