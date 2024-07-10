package ru.hh.nab.hibernate;

import jakarta.inject.Named;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.PropertiesUtils;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.hibernate.monitoring.HibernateStatisticsSender;
import ru.hh.nab.hibernate.properties.HibernatePropertiesProvider;
import ru.hh.nab.jpa.NabJpaProdConfig;
import ru.hh.nab.metrics.StatsDSender;

@Configuration
@Import({
    NabHibernateCommonConfig.class,
    NabJpaProdConfig.class
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
      SessionFactory sessionFactory,
      StatsDSender statsDSender
  ) {
    return new HibernateStatisticsSender(
        hibernatePropertiesProvider.get(),
        serviceName,
        sessionFactory,
        statsDSender
    );
  }
}
