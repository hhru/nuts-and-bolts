package ru.hh.nab.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.core.CoreProdConfig;
import ru.hh.nab.hibernate.HibernateProdConfig;

@Configuration
@Import({CoreProdConfig.class, HibernateProdConfig.class})
public class ExampleProdConfig {

  @Bean
  String serviceName() {
    return "nab-example";
  }

  @Bean
  ExampleResource exampleResource() {
    return new ExampleResource();
  }
}
