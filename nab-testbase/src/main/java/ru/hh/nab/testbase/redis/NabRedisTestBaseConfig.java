package ru.hh.nab.testbase.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NabRedisTestBaseConfig {

  @Bean
  EmbeddedRedisFactory embeddedRedisFactory() {
    return new EmbeddedRedisFactory();
  }

}
