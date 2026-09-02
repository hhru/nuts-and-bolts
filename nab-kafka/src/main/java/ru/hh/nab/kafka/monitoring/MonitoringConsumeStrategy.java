package ru.hh.nab.kafka.monitoring;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import ru.hh.metrics.MetricsSender;
import ru.hh.metrics.timinglogger.Timings;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerMetadata;

public class MonitoringConsumeStrategy<T> implements ConsumeStrategy<T> {

  private final Timings timings;
  private final ConsumeStrategy<T> consumeStrategy;

  private final AtomicLong processingId = new AtomicLong(0);
  private final ConsumerMetadata consumerMetadata;

  public MonitoringConsumeStrategy(
      MetricsSender metricsSender,
      ConsumerMetadata consumerMetadata,
      ConsumeStrategy<T> consumeStrategy
  ) {
    this.consumerMetadata = consumerMetadata;
    this.timings = buildTimings(metricsSender, consumerMetadata);
    this.consumeStrategy = consumeStrategy;
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException {
    addMdcData(messages);
    timings.resetTime();
    consumeStrategy.onMessagesBatch(messages, ack);
    timings.time();
  }

  private void addMdcData(List<ConsumerRecord<String, T>> messages) {
    String partitions = messages.stream().map(ConsumerRecord::partition).distinct().map(Object::toString).collect(Collectors.joining(","));
    MDC.put("topic", consumerMetadata.getTopic());
    MDC.put("operation", consumerMetadata.getOperation());
    MDC.put("processingId", String.valueOf(processingId.addAndGet(1L)));
    MDC.put("partitions", partitions);
    MDC.put("batchSize", String.valueOf(messages.size()));
  }

  private static Timings buildTimings(MetricsSender metricsSender, ConsumerMetadata identifier) {
    Timings.Builder builder = new Timings.Builder()
        .withMetric("batchProcessingTime")
        .withMetricsSender(metricsSender);
    identifier.toMetricTags().forEach(builder::withTag);
    return builder.start();
  }
}
