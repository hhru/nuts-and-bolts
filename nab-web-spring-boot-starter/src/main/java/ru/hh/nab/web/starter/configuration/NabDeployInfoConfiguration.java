package ru.hh.nab.web.starter.configuration;

import jakarta.inject.Named;
import java.util.List;
import static java.util.Objects.requireNonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTERS;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_VERSION;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.util.EnvironmentUtils;

@Configuration
@EnableConfigurationProperties(InfrastructureProperties.class)
public class NabDeployInfoConfiguration {

  @Named(SERVICE_NAME)
  @Bean(SERVICE_NAME)
  public String serviceName(InfrastructureProperties infrastructureProperties) {
    return infrastructureProperties.getServiceName();
  }

  @Named(SERVICE_VERSION)
  @Bean(SERVICE_VERSION)
  public String serviceVersion(BuildProperties buildProperties) {
    return requireNonNull(buildProperties.getVersion());
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
    return new FileSettings(EnvironmentUtils.getProperties(environment));
  }
}
