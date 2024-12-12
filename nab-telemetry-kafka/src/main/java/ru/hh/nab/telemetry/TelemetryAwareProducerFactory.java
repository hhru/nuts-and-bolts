package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.kafka.clients.CommonClientConfigs;
import org.springframework.kafka.core.KafkaTemplate;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaProducerFactory;
import ru.hh.nab.kafka.producer.SerializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;

public class TelemetryAwareProducerFactory extends KafkaProducerFactory {
  private final OpenTelemetry telemetry;

  public TelemetryAwareProducerFactory(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier,
      OpenTelemetry telemetry,
      Supplier<String> bootstrapServersSupplier
  ) {
    super(configProvider, serializerSupplier, bootstrapServersSupplier);

    this.telemetry = telemetry;
  }

  public TelemetryAwareProducerFactory(
      ConfigProvider configProvider,
      SerializerSupplier serializerSupplier,
      OpenTelemetry telemetry
  ) {
    super(configProvider, serializerSupplier);

    this.telemetry = telemetry;
  }

  protected KafkaProducer prepare(KafkaTemplate<String, Object> template) {
    Map<String, Object> producerConfig = configProvider.getProducerConfig(this.producerName);
    // Presence of a client id verified at ru.hh.nab.kafka.producer.KafkaProducerFactory#validateConfig
    String clientId = producerConfig.get(CommonClientConfigs.CLIENT_ID_CONFIG).toString();
    return new TelemetryKafkaProducerWrapper(
        super.prepare(template), telemetry, configProvider.getKafkaClusterName(), clientId);
  }
}
