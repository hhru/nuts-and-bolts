package ru.hh.nab.kafka.producer;

import java.util.function.Supplier;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import ru.hh.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_NAME;

public class KafkaProducerFactory {

  protected final ConfigProvider configProvider;
  private final SerializerSupplier serializerSupplier;
  private Supplier<String> bootstrapSupplier;

  public KafkaProducerFactory(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier
  ) {
    this.configProvider = configProvider;
    this.serializerSupplier = serializerSupplier;
  }

  public KafkaProducerFactory(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier,
      Supplier<String> bootstrapSupplier
  ) {
    this(configProvider, serializerSupplier);
    this.bootstrapSupplier = bootstrapSupplier;

  }

  public KafkaProducer createDefaultProducer() {
    return createProducer(DEFAULT_PRODUCER_NAME);
  }

  public KafkaProducer createProducer(String producerSettingsName) {
    var producerConfig = configProvider.getProducerConfig(producerSettingsName);
    producerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(
        producerConfig,
        new StringSerializer(),
        serializerSupplier.supply()
    );

    producerFactory.setBootstrapServersSupplier(this.bootstrapSupplier);

    return prepare(new KafkaTemplate<>(producerFactory));
  }

  protected KafkaProducer prepare(KafkaTemplate<String, Object> template) {
    return new DefaultKafkaProducer(template);
  }
}
