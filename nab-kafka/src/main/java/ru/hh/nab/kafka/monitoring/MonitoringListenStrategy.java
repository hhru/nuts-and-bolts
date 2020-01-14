package ru.hh.nab.kafka.monitoring;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.metrics.timinglogger.Timings;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ListenStrategy;
import ru.hh.nab.kafka.consumer.ListenerGroupId;
import ru.hh.nab.metrics.StatsDSender;
import java.util.List;

public class MonitoringListenStrategy<T> implements ListenStrategy<T> {

  private final Timings timings;
  private final ListenStrategy<T> listenStrategy;

  public MonitoringListenStrategy(StatsDSender statsDSender,
                                  ListenerGroupId listenerGroupId,
                                  ListenStrategy<T> listenStrategy) {
    this.timings = buildTimings(statsDSender, listenerGroupId);
    this.listenStrategy = listenStrategy;
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack ack) {
    timings.resetTime();
    listenStrategy.onMessagesBatch(messages, ack);
    timings.time();
  }

  private Timings buildTimings(StatsDSender statsDSender, ListenerGroupId identifier) {
    Timings.Builder builder = new Timings.Builder()
        .withMetric("batchProcessingTimeMs")
        .withStatsDSender(statsDSender);
    identifier.toMetricTags().forEach(builder::withTag);
    return builder.start();
  }
}
