package ru.hh.nab.telemetry.kafka;

import io.opentelemetry.api.OpenTelemetry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import ru.hh.metrics.MetricsSender;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerMetadata;
import ru.hh.nab.kafka.consumer.DefaultConsumerFactory;
import ru.hh.nab.kafka.consumer.DeserializerSupplier;
import ru.hh.nab.kafka.util.ConfigProvider;

public class TelemetryAwareConsumerFactory extends DefaultConsumerFactory {
  private final OpenTelemetry telemetry;

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      MetricsSender metricsSender,
      Logger logger,
      OpenTelemetry telemetry,
      Supplier<String> bootstrapServersSupplier
  ) {
    super(configProvider, deserializerSupplier, metricsSender, logger, bootstrapServersSupplier);

    this.telemetry = telemetry;
  }

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      MetricsSender metricsSender,
      OpenTelemetry telemetry,
      Supplier<String> bootstrapServersSupplier
  ) {
    super(configProvider, deserializerSupplier, metricsSender, bootstrapServersSupplier);

    this.telemetry = telemetry;
  }

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      MetricsSender metricsSender,
      Logger logger,
      OpenTelemetry telemetry
  ) {
    super(configProvider, deserializerSupplier, metricsSender, logger);

    this.telemetry = telemetry;
  }

  public TelemetryAwareConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      MetricsSender metricsSender,
      OpenTelemetry telemetry
  ) {
    super(configProvider, deserializerSupplier, metricsSender);

    this.telemetry = telemetry;
  }

  @Override
  public <T> ConsumeStrategy<T> interceptConsumeStrategy(ConsumerMetadata consumerMetadata, ConsumeStrategy<T> consumeStrategy) {
    return new TelemetryConsumeStrategyWrapper<>(
        configProvider.getKafkaClusterName(), super.interceptConsumeStrategy(consumerMetadata, consumeStrategy), consumerMetadata, telemetry
    );
  }
}
