package ru.hh.nab.example;

import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.testbase.NabTestBase;

@Configuration
public class ExampleTestConfig {

  @Bean
  Function<String, String> serverPortAwareBean(NabTestBase.NabTestContext ctx) {
    return path -> ctx.baseUrl() + path;
  }
}
