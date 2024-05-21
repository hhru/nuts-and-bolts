package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerDescription;
import ru.hh.nab.kafka.consumer.DefaultConsumerFactory;
import ru.hh.nab.kafka.consumer.DeserializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.metrics.StatsDSender;

public class TelemetryAwareConsumerFactory extends DefaultConsumerFactory {
  private final OpenTelemetry telemetry;

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      Logger logger,
      OpenTelemetry telemetry,
      Supplier<String> bootstrapServersSupplier
  ) {
    super(configProvider, deserializerSupplier, statsDSender, logger, bootstrapServersSupplier);

    this.telemetry = telemetry;
  }

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      OpenTelemetry telemetry,
      Supplier<String> bootstrapServersSupplier
  ) {
    super(configProvider, deserializerSupplier, statsDSender, bootstrapServersSupplier);

    this.telemetry = telemetry;
  }

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      Logger logger,
      OpenTelemetry telemetry
  ) {
    super(configProvider, deserializerSupplier, statsDSender, logger);

    this.telemetry = telemetry;
  }

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      OpenTelemetry telemetry
  ) {
    super(configProvider, deserializerSupplier, statsDSender);

    this.telemetry = telemetry;
  }

  @Override
  public <T> ConsumeStrategy<T> interceptConsumeStrategy(ConsumerDescription consumerDescription, ConsumeStrategy<T> consumeStrategy) {
    return new TelemetryConsumeStrategyWrapper<>(
        configProvider.getKafkaClusterName(), super.interceptConsumeStrategy(consumerDescription, consumeStrategy), consumerDescription, telemetry
    );
  }
}
