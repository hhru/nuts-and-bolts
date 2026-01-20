package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_CLIENT_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_KAFKA_MESSAGE_KEY;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MessagingSystemIncubatingValues.KAFKA;
import static io.opentelemetry.semconv.incubating.PeerIncubatingAttributes.PEER_SERVICE;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaSendResult;
import static ru.hh.nab.telemetry.semconv.KafkaSemanticAttributesForRemoval.MESSAGING_DESTINATION_KIND;
import static ru.hh.nab.telemetry.semconv.KafkaSemanticAttributesForRemoval.MESSAGING_KAFKA_CLIENT_ID;

public class TelemetryKafkaProducerWrapper extends KafkaProducer {
  private final String clusterName;
  private final String clientId;
  private final KafkaProducer producer;
  private final Tracer tracer;
  private final KafkaTelemetryPropagator propagator;

  TelemetryKafkaProducerWrapper(KafkaProducer producer, OpenTelemetry telemetry, String clusterName, String clientId) {
    this.producer = producer;
    this.clusterName = clusterName;
    this.clientId = clientId;
    this.tracer = telemetry.getTracer("kafka");
    this.propagator = new KafkaTelemetryPropagator(telemetry);
  }

  @Override
  public <T> CompletableFuture<KafkaSendResult<T>> sendMessage(ProducerRecord<String, T> record, Executor executor) {
    SpanBuilder builder = tracer
        .spanBuilder(record.topic() + " send")
        .setParent(Context.current())
        .setSpanKind(SpanKind.PRODUCER)
        .setAttribute(PEER_SERVICE, clusterName)
        .setAttribute(MessagingIncubatingAttributes.MESSAGING_SYSTEM, KAFKA)
        .setAttribute(MESSAGING_DESTINATION_NAME, record.topic())
        .setAttribute(MESSAGING_DESTINATION_KIND, "topic")

        .setAttribute(MESSAGING_KAFKA_CLIENT_ID, clientId)
        .setAttribute(MESSAGING_CLIENT_ID, clientId);

    if (record.key() != null) {
      builder.setAttribute(MESSAGING_KAFKA_MESSAGE_KEY, record.key());
    }

    var span = builder.startSpan();

    try (Scope ignore = span.makeCurrent()) {
      propagator.propagate(record.headers());
    }

    return producer
        .sendMessage(record, executor)
        .whenComplete((kafkaSendResult, throwable) -> {
          if (throwable != null) {
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
          } else {
            span.setStatus(StatusCode.OK);
          }

          span.end();
        });
  }
}
