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
import java.util.Properties;

@Configuration
@Import({NabKafkaJsonTestConfig.class})
public class KafkaTestConfig {

  @Bean
  ObjectMapper objectMapper(){
    return new ObjectMapper();
  }

  @Bean
  DeserializerSupplier deserializerSupplier() {
    return new JacksonDeserializerSupplier(objectMapper());
  }

  @Bean
  ConfigProvider configProvider(Properties serviceProperties) {
    return new ConfigProvider("service", "kafka", new FileSettings(serviceProperties));
  }

  @Bean
  ListenerFactory listenerFactory(ConfigProvider configProvider) {
    return new DefaultListenerFactory(configProvider, deserializerSupplier(), null);
  }

  @Bean
  SerializerSupplier serializerSupplier() {
    return new JacksonSerializerSupplier(objectMapper());
  }

  @Bean
  PublisherFactory kafkaPublisher(ConfigProvider configProvider, SerializerSupplier serializerSupplier) {
    return new PublisherFactory(configProvider, serializerSupplier);
  }
}
