package ru.hh.nab.kafka.monitoring;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.metrics.timinglogger.Timings;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerGroupId;
import ru.hh.nab.metrics.StatsDSender;

public class MonitoringConsumeStrategy<T> implements ConsumeStrategy<T> {

  private final Timings timings;
  private final ConsumeStrategy<T> consumeStrategy;

  public MonitoringConsumeStrategy(StatsDSender statsDSender,
                                   ConsumerGroupId consumerGroupId,
                                   ConsumeStrategy<T> consumeStrategy) {
    this.timings = buildTimings(statsDSender, consumerGroupId);
    this.consumeStrategy = consumeStrategy;
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException {
    timings.resetTime();
    consumeStrategy.onMessagesBatch(messages, ack);
    timings.time();
  }

  private Timings buildTimings(StatsDSender statsDSender, ConsumerGroupId identifier) {
    Timings.Builder builder = new Timings.Builder()
        .withMetric("batchProcessingTimeMs")
        .withStatsDSender(statsDSender);
    identifier.toMetricTags().forEach(builder::withTag);
    return builder.start();
  }
}
