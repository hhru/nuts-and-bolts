package ru.hh.nab.kafka.publisher;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.hh.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.kafka.util.ConfigProvider;

public class PublisherFactory {

  private final ConfigProvider configProvider;
  private final SerializerSupplier serializerSupplier;

  public PublisherFactory(ConfigProvider configProvider,
                          SerializerSupplier serializerSupplier) {
    this.configProvider = configProvider;
    this.serializerSupplier = serializerSupplier;
  }

  public <T> Publisher<T> createForTopic(String topicName) {
    var producerConfig = configProvider.getProducerConfig(topicName);
    producerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    ProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(
        producerConfig,
        new StringSerializer(),
        serializerSupplier.supply()
    );

    return new Publisher<>(topicName, new KafkaTemplate<>(producerFactory));
  }
}
