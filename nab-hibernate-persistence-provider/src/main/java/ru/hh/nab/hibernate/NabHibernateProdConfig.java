package ru.hh.nab.hibernate;

import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.PropertiesUtils;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.datasource.NabDataSourceProdConfig;
import ru.hh.nab.hibernate.monitoring.HibernateStatisticsSender;
import ru.hh.nab.hibernate.properties.HibernatePropertiesProvider;
import ru.hh.nab.metrics.StatsDSender;

@Configuration
@Import({
    NabHibernateCommonConfig.class,
    NabDataSourceProdConfig.class
})
public class NabHibernateProdConfig {

  @Bean
  HibernatePropertiesProvider hibernatePropertiesProvider() throws Exception {
    Properties hibernateProperties = PropertiesUtils.fromFilesInSettingsDir("hibernate.properties", "hibernate.properties.dev");
    return new HibernatePropertiesProvider(hibernateProperties);
  }

  @Bean
  HibernateStatisticsSender hibernateStatisticsSender(
      HibernatePropertiesProvider hibernatePropertiesProvider,
      @Named(SERVICE_NAME) String serviceName,
      EntityManagerFactory entityManagerFactory,
      StatsDSender statsDSender
  ) {
    return new HibernateStatisticsSender(
        hibernatePropertiesProvider.get(),
        serviceName,
        entityManagerFactory.unwrap(SessionFactoryImpl.class),
        statsDSender
    );
  }
}
