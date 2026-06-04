package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_CLIENT_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_CONSUMER_GROUP_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION_TYPE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MessagingOperationTypeIncubatingValues.PROCESS;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MessagingSystemIncubatingValues.KAFKA;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerMetadata;
import static ru.hh.nab.telemetry.semconv.KafkaSemanticAttributesForRemoval.MESSAGING_DESTINATION_KIND;
import static ru.hh.nab.telemetry.semconv.KafkaSemanticAttributesForRemoval.MESSAGING_KAFKA_CLIENT_ID;
import static ru.hh.nab.telemetry.semconv.KafkaSemanticAttributesForRemoval.MESSAGING_KAFKA_CONSUMER_GROUP;
import static ru.hh.nab.telemetry.semconv.KafkaSemanticAttributesForRemoval.MESSAGING_OPERATION;

public class TelemetryConsumeStrategyWrapper<T> implements ConsumeStrategy<T> {

  private final String clusterName;
  private final ConsumeStrategy<T> consumeStrategy;
  private final ConsumerMetadata consumerMetadata;
  private final Tracer tracer;
  private final KafkaTelemetryPropagator propagator;

  TelemetryConsumeStrategyWrapper(
      String clusterName,
      ConsumeStrategy<T> consumeStrategy,
      ConsumerMetadata consumerMetadata,
      OpenTelemetry telemetry
  ) {
    this.clusterName = clusterName;
    this.consumeStrategy = consumeStrategy;
    this.consumerMetadata = consumerMetadata;
    this.tracer = telemetry.getTracer("kafka");
    this.propagator = new KafkaTelemetryPropagator(telemetry);
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException {
    SpanBuilder builder = tracer
        .spanBuilder(consumerMetadata.getTopic() + " process")
        .setParent(Context.current())
        .setSpanKind(SpanKind.CONSUMER)
        .setAttribute(SERVICE_NAME, clusterName)
        .setAttribute(MESSAGING_SYSTEM, KAFKA)

        .setAttribute(MESSAGING_OPERATION, PROCESS)
        .setAttribute(MESSAGING_OPERATION_TYPE, PROCESS)

        .setAttribute(MESSAGING_DESTINATION_NAME, consumerMetadata.getTopic())
        .setAttribute(MESSAGING_DESTINATION_KIND, "topic")

        .setAttribute(MESSAGING_KAFKA_CLIENT_ID, consumerMetadata.getServiceName())
        .setAttribute(MESSAGING_CLIENT_ID, consumerMetadata.getServiceName())

        .setAttribute(MESSAGING_KAFKA_CONSUMER_GROUP, consumerMetadata.getConsumerGroupId())
        .setAttribute(MESSAGING_CONSUMER_GROUP_NAME, consumerMetadata.getConsumerGroupId());

    messages.forEach(record -> {
      Context extractedContext = propagator.getTelemetryContext(Context.current(), record.headers());
      builder.addLink(Span.fromContext(extractedContext).getSpanContext());
    });

    var span = builder.startSpan();

    try (Scope ignored = span.makeCurrent()) {
      consumeStrategy.onMessagesBatch(messages, ack);
    } finally {
      span.setStatus(StatusCode.OK);
      span.end();
    }
  }
}
