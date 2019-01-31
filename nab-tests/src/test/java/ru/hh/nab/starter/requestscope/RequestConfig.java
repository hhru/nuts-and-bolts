package ru.hh.nab.starter.requestscope;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.annotation.RequestScope;
import ru.hh.nab.testbase.NabTestConfig;

@Configuration
@Import({
    NabTestConfig.class,
})
public class RequestConfig {

  @Bean
  @RequestScope
  RequestDetails requestDetails() {
    return new RequestDetails();
  }
}
