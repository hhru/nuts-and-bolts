package ru.hh.nab.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.kafka.test.KafkaTestUtils;
import ru.hh.kafka.test.TestKafka;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.listener.DefaultListenerFactory;
import ru.hh.nab.kafka.listener.DeserializerSupplier;
import ru.hh.nab.kafka.listener.ListenerFactory;
import ru.hh.nab.kafka.publisher.PublisherFactory;
import ru.hh.nab.kafka.publisher.SerializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonDeserializerSupplier;
import ru.hh.nab.kafka.serialization.JacksonSerializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class KafkaTestConfig {

  @Bean
  public TestKafkaWithJsonMessages testKafka() {
    return KafkaTestUtils.startKafkaWithJsonMessages(new ObjectMapper());
  }

  @Bean
  Properties serviceProperties(TestKafka testKafka) throws IOException {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocation(new ClassPathResource("service-test.properties"));
    propertiesFactoryBean.afterPropertiesSet();
    Properties properties = propertiesFactoryBean.getObject();
    properties.setProperty("kafka.common.bootstrap.servers", testKafka.getBootstrapServers());
    return properties;
  }

  @Bean
  DeserializerSupplier deserializerSupplier() {
    return new JacksonDeserializerSupplier(new ObjectMapper());
  }

  @Bean
  ConfigProvider configProvider(Properties serviceProperties) {
    return new ConfigProvider("service", "kafka", new FileSettings(serviceProperties));
  }

  @Bean
  ListenerFactory listenerFactory(ConfigProvider configProvider, DeserializerSupplier deserializerSupplier) {
    return new DefaultListenerFactory(configProvider, deserializerSupplier, null);
  }

  @Bean
  SerializerSupplier serializerSupplier() {
    return new JacksonSerializerSupplier(new ObjectMapper());
  }

  @Bean
  PublisherFactory kafkaPublisher(ConfigProvider configProvider, SerializerSupplier serializerSupplier) {
    return new PublisherFactory(configProvider, serializerSupplier);
  }
}
