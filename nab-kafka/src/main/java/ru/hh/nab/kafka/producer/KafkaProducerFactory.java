package ru.hh.nab.kafka.producer;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.hh.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_NAME;

public class KafkaProducerFactory {

  private final ConfigProvider configProvider;
  private final SerializerSupplier serializerSupplier;

  public KafkaProducerFactory(ConfigProvider configProvider,
                              SerializerSupplier serializerSupplier) {
    this.configProvider = configProvider;
    this.serializerSupplier = serializerSupplier;
  }

  public <T> KafkaProducer<T> createDefaultProducer() {
    return createProducer(DEFAULT_PRODUCER_NAME);
  }

  public <T> KafkaProducer<T> createProducer(String producerSettingsName) {
    var producerConfig = configProvider.getProducerConfig(producerSettingsName);
    producerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    ProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(
        producerConfig,
        new StringSerializer(),
        serializerSupplier.supply()
    );

    return new DefaultKafkaProducer<>(new KafkaTemplate<>(producerFactory));
  }
}
