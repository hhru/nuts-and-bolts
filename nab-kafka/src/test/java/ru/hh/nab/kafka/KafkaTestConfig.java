package ru.hh.nab.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Properties;
import static org.mockito.Mockito.mock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.kafka.test.KafkaTestUtils;
import ru.hh.kafka.test.TestKafka;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.consumer.DefaultConsumerFactory;
import ru.hh.nab.kafka.consumer.DeserializerSupplier;
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;
import ru.hh.nab.kafka.producer.KafkaProducerFactory;
import ru.hh.nab.kafka.producer.SerializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonDeserializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonSerializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.metrics.StatsDSender;

@Configuration
public class KafkaTestConfig {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TestKafkaWithJsonMessages TEST_KAFKA = KafkaTestUtils.startKafkaWithJsonMessages(
      OBJECT_MAPPER,
      Map.of("num.partitions", "5")
  );

  @Bean
  public TestKafkaWithJsonMessages testKafka() {
    return TEST_KAFKA;
  }

  @Bean
  public DeserializerSupplier deserializerSupplier() {
    return new JacksonDeserializerSupplier(OBJECT_MAPPER);
  }

  @Bean
  public ConfigProvider configProvider(Properties properties, StatsDSender statsDSender) {
    return new ConfigProvider("service", "kafka", properties, statsDSender);
  }

  @Bean
  public KafkaConsumerFactory consumerFactory(
      ConfigProvider configProvider,
      StatsDSender statsDSender,
      DeserializerSupplier deserializerSupplier,
      TestKafka testKafka
  ) {
    return new DefaultConsumerFactory(configProvider, deserializerSupplier, statsDSender, testKafka::getBootstrapServers);
  }

  @Bean
  public SerializerSupplier serializerSupplier() {
    return new JacksonSerializerSupplier(new ObjectMapper());
  }

  @Bean
  public KafkaProducerFactory kafkaProducer(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier,
      TestKafka testKafka
  ) {
    return new KafkaProducerFactory(configProvider, serializerSupplier, testKafka::getBootstrapServers);
  }

  @Bean
  public Properties properties() {
    Properties properties = new Properties();
    properties.put("kafka.common.security.protocol", "PLAINTEXT");
    properties.put("kafka.producer.default.retries", "3");
    properties.put("kafka.producer.default.linger.ms", "10");
    properties.put("kafka.producer.default.batch.size", "1");
    properties.put("kafka.consumer.default.auto.offset.reset", "earliest");
    properties.put("kafka.consumer.default.fetch.max.wait.ms", "250");
    properties.put("kafka.consumer.default.max.poll.interval.ms", "5000");
    properties.put("kafka.consumer.default.max.poll.records", "25");
    properties.put("kafka.consumer.default.nab_setting.poll.timeout.ms", "500");
    properties.put("kafka.consumer.default.nab_setting.backoff.initial.interval", "1");
    properties.put("kafka.consumer.default.nab_setting.backoff.max.interval", "1");
    return properties;
  }

  @Bean
  public StatsDSender statsDSender() {
    return mock(StatsDSender.class);
  }
}
