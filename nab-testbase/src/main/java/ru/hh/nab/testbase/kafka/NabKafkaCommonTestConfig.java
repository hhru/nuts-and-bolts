package ru.hh.nab.testbase.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.hh.kafka.test.TestKafka;
import static ru.hh.nab.testbase.NabTestConfig.createProperties;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class NabKafkaCommonTestConfig {

  @Bean
  @Primary
  Properties serviceProperties(TestKafka testKafka) throws IOException {
    Properties serviceProperties = createProperties("service-test.properties");
    serviceProperties.putAll(createProperties("kafka-test.properties"));
    serviceProperties.setProperty("kafka.common.bootstrap.servers", testKafka.getBootstrapServers());
    return serviceProperties;
  }
}
