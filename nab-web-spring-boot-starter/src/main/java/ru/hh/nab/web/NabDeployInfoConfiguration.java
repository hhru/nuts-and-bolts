package ru.hh.nab.web;

import jakarta.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTERS;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.starter.AppMetadata;

@Configuration
@EnableConfigurationProperties(InfrastructureProperties.class)
public class NabDeployInfoConfiguration {

  @Named(SERVICE_NAME)
  @Bean(SERVICE_NAME)
  public String serviceName(InfrastructureProperties infrastructureProperties) {
    return infrastructureProperties.getServiceName();
  }

  @Named(DATACENTER)
  @Bean(DATACENTER)
  public String datacenter(InfrastructureProperties infrastructureProperties) {
    return infrastructureProperties.getDatacenter();
  }

  @Named(DATACENTERS)
  @Bean(DATACENTERS)
  public List<String> datacenters(InfrastructureProperties infrastructureProperties) {
    return infrastructureProperties.getDatacenters();
  }

  @Named(NODE_NAME)
  @Bean(NODE_NAME)
  public String nodeName(InfrastructureProperties infrastructureProperties) {
    return infrastructureProperties.getNodeName();
  }

  @Bean
  public FileSettings fileSettings(ConfigurableEnvironment environment) {
    Properties properties = new Properties();
    environment
        .getPropertySources()
        .stream()
        .filter(source -> source instanceof EnumerablePropertySource<?>)
        .map(source -> ((EnumerablePropertySource<?>) source).getPropertyNames())
        .flatMap(Arrays::stream)
        .distinct()
        .forEach(propertyName -> properties.setProperty(propertyName, environment.getProperty(propertyName)));
    return new FileSettings(properties);
  }

  @Bean
  public PropertiesFactoryBean projectProperties() {
    PropertiesFactoryBean projectProps = new PropertiesFactoryBean();
    projectProps.setLocation(new ClassPathResource(AppMetadata.PROJECT_PROPERTIES));
    projectProps.setIgnoreResourceNotFound(true);
    return projectProps;
  }

  @Bean
  public AppMetadata appMetadata(String serviceName, Properties projectProperties) {
    return new AppMetadata(serviceName, projectProperties);
  }
}
