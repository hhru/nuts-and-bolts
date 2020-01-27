package ru.hh.nab.testbase.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.kafka.test.KafkaTestUtils;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;

@Configuration
@Import({NabKafkaCommonTestConfig.class})
public class NabKafkaJsonTestConfig {

  @Bean
  public TestKafkaWithJsonMessages testKafka(TestObjectMapperSupplier objectMapperSupplier) {
    return KafkaTestUtils.startKafkaWithJsonMessages(objectMapperSupplier.get());
  }
}
