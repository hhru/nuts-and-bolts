package ru.hh.nab.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
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
import ru.hh.nab.starter.qualifier.Service;

@Configuration
public class KafkaTestConfig {

  @Bean
  public TestKafkaWithJsonMessages testKafka() {
    return KafkaTestUtils.startKafkaWithJsonMessages(new ObjectMapper(), Map.of("num.partitions", "5"));
  }

  @Bean
  @Service
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
  ConfigProvider configProvider(@Service Properties serviceProperties) {
    return new ConfigProvider("service", "kafka", new FileSettings(serviceProperties));
  }

  @Bean
  KafkaConsumerFactory consumerFactory(ConfigProvider configProvider, DeserializerSupplier deserializerSupplier) {
    return new DefaultConsumerFactory(configProvider, deserializerSupplier, null);
  }

  @Bean
  SerializerSupplier serializerSupplier() {
    return new JacksonSerializerSupplier(new ObjectMapper());
  }

  @Bean
  KafkaProducerFactory kafkaProducer(ConfigProvider configProvider, SerializerSupplier serializerSupplier) {
    return new KafkaProducerFactory(configProvider, serializerSupplier);
  }
}
