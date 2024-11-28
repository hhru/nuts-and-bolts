package ru.hh.nab.testbase.hibernate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import ru.hh.nab.hibernate.NabHibernateCommonConfig;
import ru.hh.nab.hibernate.properties.HibernatePropertiesProvider;
import ru.hh.nab.testbase.datasource.NabDataSourceTestBaseConfig;
import ru.hh.nab.web.starter.util.EnvironmentUtils;

@Configuration
@Import({
    NabDataSourceTestBaseConfig.class,
    NabHibernateCommonConfig.class,
})
public class NabHibernateTestBaseConfig {

  @Bean
  HibernatePropertiesProvider hibernatePropertiesProvider(ConfigurableEnvironment environment) {
    return new HibernatePropertiesProvider(EnvironmentUtils.getPropertiesStartWith(environment, "hibernate"));
  }
}
