package ru.hh.nab.web;

import jakarta.inject.Named;
import java.io.IOException;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.function.Predicate;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.qualifier.Service;

@Configuration
public class NabDeployInfoConfiguration {

  private static final String NODE_NAME_ENV = "NODE_NAME";
  private static final String PROPERTIES_FILE_NAME = "service.properties";

  @Named(SERVICE_NAME)
  @Bean(SERVICE_NAME)
  public String serviceName(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(SERVICE_NAME))
        .filter(Predicate.not(String::isEmpty))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", SERVICE_NAME)));
  }

  @Named(DATACENTER)
  @Bean(DATACENTER)
  public String datacenter(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(DATACENTER))
        .filter(Predicate.not(String::isEmpty))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", DATACENTER)));
  }

  @Named(NODE_NAME)
  @Bean(NODE_NAME)
  public String nodeName(FileSettings fileSettings) {
    return ofNullable(System.getenv(NODE_NAME_ENV))
        .orElseGet(
            () -> ofNullable(fileSettings.getString(NODE_NAME))
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", NODE_NAME)))
        );
  }

  @Bean
  public FileSettings fileSettings(@Service Properties serviceProperties) {
    return new FileSettings(serviceProperties);
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

  @Bean
  @Service
  public Properties serviceProperties() throws IOException {
    return fromFilesInSettingsDir(PROPERTIES_FILE_NAME);
  }
}
