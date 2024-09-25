package ru.hh.nab.kafka.producer;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_NAME;

public class KafkaProducerFactory {

  protected final ConfigProvider configProvider;
  private final SerializerSupplier serializerSupplier;
  private final Supplier<String> bootstrapServersSupplier;

  public KafkaProducerFactory(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier
  ) {
    this(configProvider, serializerSupplier, null);
  }

  public KafkaProducerFactory(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier,
      @Nullable Supplier<String> bootstrapServersSupplier
  ) {
    validateConfig(configProvider, bootstrapServersSupplier, serializerSupplier);
    this.configProvider = configProvider;
    this.serializerSupplier = serializerSupplier;
    this.bootstrapServersSupplier = bootstrapServersSupplier;
  }

  private static void validateConfig(
      ConfigProvider configProvider,
      Supplier<String> bootstrapServersSupplier,
      SerializerSupplier serializerSupplier
  ) {
    Map<String, Object> producerConfig = configProvider.getProducerConfig(DEFAULT_PRODUCER_NAME);
    if ((bootstrapServersSupplier == null) == isNullOrBlankString(producerConfig, CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
      throw new IllegalArgumentException("Either specify 'bootstrap.servers' in config or provide bootstrapServersSupplier to this factory");
    }
    if (isNullOrBlankString(producerConfig, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)) {
      throw new IllegalArgumentException("Required config '" + CommonClientConfigs.SECURITY_PROTOCOL_CONFIG + "' is missing");
    }
    Objects.requireNonNull(serializerSupplier);
  }

  private static boolean isNullOrBlankString(Map<String, Object> config, String key) {
    Object value = config.get(key);
    return !(value instanceof String) || ((String) value).isBlank();
  }

  public KafkaProducer createDefaultProducer() {
    return createProducer(DEFAULT_PRODUCER_NAME);
  }

  public KafkaProducer createProducer(String producerSettingsName) {
    DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(
        configProvider.getProducerConfig(producerSettingsName),
        new StringSerializer(),
        serializerSupplier.supply()
    );

    producerFactory.setBootstrapServersSupplier(this.bootstrapServersSupplier);

    return prepare(new KafkaTemplate<>(producerFactory));
  }

  protected KafkaProducer prepare(KafkaTemplate<String, Object> template) {
    return new DefaultKafkaProducer(template);
  }
}
