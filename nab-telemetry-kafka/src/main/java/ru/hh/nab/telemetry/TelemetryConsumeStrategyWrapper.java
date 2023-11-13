package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_DESTINATION_KIND;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_KAFKA_CLIENT_ID;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_KAFKA_CONSUMER_GROUP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MessagingDestinationKindValues.TOPIC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MessagingOperationValues.PROCESS;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerGroupId;

public class TelemetryConsumeStrategyWrapper<T> implements ConsumeStrategy<T> {

  private final String clusterName;
  private final ConsumeStrategy<T> consumeStrategy;
  private final ConsumerGroupId consumerGroupId;
  private final Tracer tracer;
  private final KafkaTelemetryPropagator propagator;

  TelemetryConsumeStrategyWrapper(String clusterName, ConsumeStrategy<T> consumeStrategy,
                                  ConsumerGroupId consumerGroupId, OpenTelemetry telemetry) {
    this.clusterName = clusterName;
    this.consumeStrategy = consumeStrategy;
    this.consumerGroupId = consumerGroupId;
    this.tracer = telemetry.getTracer("kafka");
    this.propagator = new KafkaTelemetryPropagator(telemetry);
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException {
    SpanBuilder builder = tracer
        .spanBuilder(consumerGroupId.getTopic() + " process")
        .setParent(Context.current())
        .setSpanKind(SpanKind.CONSUMER)
        .setAttribute(SERVICE_NAME, clusterName)
        .setAttribute(MESSAGING_SYSTEM, "kafka")
        .setAttribute(MESSAGING_OPERATION, PROCESS)
        .setAttribute(MESSAGING_DESTINATION_NAME, consumerGroupId.getTopic())
        .setAttribute(MESSAGING_DESTINATION_KIND, TOPIC)
        .setAttribute(MESSAGING_KAFKA_CLIENT_ID, consumerGroupId.getServiceName())
        .setAttribute(MESSAGING_KAFKA_CONSUMER_GROUP, consumerGroupId.toString());

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
