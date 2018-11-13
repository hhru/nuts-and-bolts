package ru.hh.nab.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class ExampleTestConfig {

  @Bean
  Function<String, String> serverPortAwareBean(String jettyBaseUrl) {
    return path -> jettyBaseUrl + path;
  }
}
