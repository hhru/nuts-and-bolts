package ru.hh.nab.kafka.producer;

import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Producer;
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

  public KafkaProducer createDefaultProducer() {
    return createProducer(DEFAULT_PRODUCER_NAME);
  }

  public KafkaProducer createProducer(String producerSettingsName) {
    var producerConfig = configProvider.getProducerConfig(producerSettingsName);
    producerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(
        producerConfig, new StringSerializer(), serializerSupplier.supply()
    ) {
      @Override
      protected Producer<String, Object> createRawProducer(Map<String, Object> configs) {
        Producer<String, Object> rawProducer = super.createRawProducer(configs);
        rawProducer.partitionsFor("request_partitions_for_topic_to_force_metadata_request");
        return rawProducer;
      }
    };

    return new DefaultKafkaProducer(new KafkaTemplate<>(producerFactory));
  }
}
