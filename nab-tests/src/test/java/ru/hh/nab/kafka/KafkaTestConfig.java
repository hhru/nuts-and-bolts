package ru.hh.nab.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.kafka.test.KafkaTestUtils;
import ru.hh.kafka.test.TestKafka;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.consumer.DefaultConsumerFactory;
import ru.hh.nab.kafka.consumer.DeserializerSupplier;
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;
import ru.hh.nab.kafka.producer.KafkaProducerFactory;
import ru.hh.nab.kafka.producer.SerializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonDeserializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonSerializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.web.NabDeployInfoConfiguration;

@Configuration
@Import(NabDeployInfoConfiguration.class)
public class KafkaTestConfig {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Bean
  public TestKafkaWithJsonMessages testKafka() {
    return KafkaTestUtils.startKafkaWithJsonMessages(OBJECT_MAPPER, Map.of("num.partitions", "5"));
  }

  @Bean
  DeserializerSupplier deserializerSupplier() {
    return new JacksonDeserializerSupplier(OBJECT_MAPPER);
  }

  @Bean
  ConfigProvider configProvider(FileSettings fileSettings) {
    return new ConfigProvider("service", "kafka", fileSettings);
  }

  @Bean
  KafkaConsumerFactory consumerFactory(ConfigProvider configProvider, DeserializerSupplier deserializerSupplier, TestKafka testKafka) {
    return new DefaultConsumerFactory(configProvider, deserializerSupplier, null, testKafka::getBootstrapServers);
  }

  @Bean
  SerializerSupplier serializerSupplier() {
    return new JacksonSerializerSupplier(new ObjectMapper());
  }

  @Bean
  KafkaProducerFactory kafkaProducer(ConfigProvider configProvider, SerializerSupplier serializerSupplier, TestKafka testKafka) {
    return new KafkaProducerFactory(configProvider, serializerSupplier, testKafka::getBootstrapServers);
  }
}
