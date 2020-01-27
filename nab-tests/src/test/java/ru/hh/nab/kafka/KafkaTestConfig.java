package ru.hh.nab.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.listener.DefaultListenerFactory;
import ru.hh.nab.kafka.listener.DeserializerSupplier;
import ru.hh.nab.kafka.listener.ListenerFactory;
import ru.hh.nab.kafka.publisher.PublisherFactory;
import ru.hh.nab.kafka.publisher.SerializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonDeserializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonSerializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.testbase.kafka.NabKafkaJsonTestConfig;
import ru.hh.nab.testbase.kafka.TestObjectMapperSupplier;
import java.util.Properties;

@Configuration
@Import({NabKafkaJsonTestConfig.class})
public class KafkaTestConfig {

  @Bean
  String serviceName() {
    return "service";
  }

  @Bean
  String kafkaClusterName() {
    return "kafka";
  }

  @Bean
  TestObjectMapperSupplier objectMapperSupplier() {
    return ObjectMapper::new;
  }

  @Bean
  DeserializerSupplier deserializerSupplier(TestObjectMapperSupplier objectMapperSupplier) {
    return new JacksonDeserializerSupplier(objectMapperSupplier.get());
  }

  @Bean
  ConfigProvider configProvider(String serviceName, String kafkaClusterName, Properties serviceProperties) {
    return new ConfigProvider(serviceName, kafkaClusterName, new FileSettings(serviceProperties));
  }

  @Bean
  ListenerFactory listenerFactory(ConfigProvider configProvider, DeserializerSupplier deserializerSupplier) {
    return new DefaultListenerFactory(configProvider, deserializerSupplier, null);
  }

  @Bean
  SerializerSupplier serializerSupplier(TestObjectMapperSupplier objectMapperSupplier) {
    return new JacksonSerializerSupplier(objectMapperSupplier.get());
  }

  @Bean
  PublisherFactory kafkaPublisher(ConfigProvider configProvider, SerializerSupplier serializerSupplier) {
    return new PublisherFactory(configProvider, serializerSupplier);
  }
}
