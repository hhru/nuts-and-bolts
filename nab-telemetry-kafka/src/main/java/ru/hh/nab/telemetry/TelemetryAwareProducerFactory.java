package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import java.util.function.Supplier;
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
    return new TelemetryKafkaProducerWrapper(
        super.prepare(template), telemetry, configProvider.getKafkaClusterName(), configProvider.getServiceName());
  }
}
