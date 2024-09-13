package ru.hh.nab.starter;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {

  public static final String CONSUL_PORT_PROPERTY = "consul.http.port";
  public static final String CONSUL_HOST_PROPERTY = "consul.http.host";
  static final String PROPERTIES_FILE_NAME = "service.properties";
}
