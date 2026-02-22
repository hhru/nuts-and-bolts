package ru.hh.nab.kafka.monitoring;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import ru.hh.metrics.timinglogger.Timings;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerMetadata;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;

public class MonitoringConsumeStrategy<T> implements ConsumeStrategy<T> {

  private final Timings timings;
  private final ConsumeStrategy<T> consumeStrategy;

  private final AtomicLong processingId = new AtomicLong(0);
  private final ConsumerMetadata consumerMetadata;

  public MonitoringConsumeStrategy(
      String serviceName,
      StatsDSender statsDSender,
      ConsumerMetadata consumerMetadata,
      ConsumeStrategy<T> consumeStrategy
  ) {
    this.consumerMetadata = consumerMetadata;
    this.timings = buildTimings(statsDSender, consumerMetadata, serviceName);
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

  private Timings buildTimings(StatsDSender statsDSender, ConsumerMetadata consumerMetadata, String serviceName) {
    return new Timings.Builder()
        .withMetric("batchProcessingTime")
        .withStatsDSender(statsDSender)
        .withTag(new Tag(APP_TAG_NAME, serviceName))
        .withTag(new Tag("topic", consumerMetadata.getTopic()))
        .withTag(new Tag("operation", consumerMetadata.getOperation()))
        .start();
  }
}
