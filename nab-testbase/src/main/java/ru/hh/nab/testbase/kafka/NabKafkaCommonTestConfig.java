package ru.hh.nab.testbase.kafka;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.kafka.test.TestKafka;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class NabKafkaCommonTestConfig {

  @Bean
  Properties serviceProperties(TestKafka testKafka, String kafkaClusterName) throws IOException {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocation(new ClassPathResource("service-test.properties"));
    propertiesFactoryBean.afterPropertiesSet();
    Properties properties = propertiesFactoryBean.getObject();
    properties.setProperty(
        String.format("%s.common.bootstrap.servers", kafkaClusterName),
        testKafka.getBootstrapServers()
    );
    return properties;
  }
}
