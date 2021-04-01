package ru.hh.nab.kafka.monitoring;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import ru.hh.metrics.timinglogger.Timings;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerGroupId;
import ru.hh.nab.metrics.StatsDSender;

public class MonitoringConsumeStrategy<T> implements ConsumeStrategy<T> {

  private final Timings timings;
  private final ConsumeStrategy<T> consumeStrategy;

  private final AtomicLong processingId = new AtomicLong(0);
  private final ConsumerGroupId consumerGroupId;

  public MonitoringConsumeStrategy(StatsDSender statsDSender,
                                   ConsumerGroupId consumerGroupId,
                                   ConsumeStrategy<T> consumeStrategy) {
    this.consumerGroupId = consumerGroupId;
    this.timings = buildTimings(statsDSender, consumerGroupId);
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
    MDC.put("topic", consumerGroupId.getTopic());
    MDC.put("operation", consumerGroupId.getOperation());
    MDC.put("processingId", String.valueOf(processingId.addAndGet(1L)));
    MDC.put("partitions", partitions);
    MDC.put("batchSize", String.valueOf(messages.size()));
  }

  private Timings buildTimings(StatsDSender statsDSender, ConsumerGroupId identifier) {
    Timings.Builder builder = new Timings.Builder()
        .withMetric("batchProcessingTimeMs")
        .withStatsDSender(statsDSender);
    identifier.toMetricTags().forEach(builder::withTag);
    return builder.start();
  }
}
