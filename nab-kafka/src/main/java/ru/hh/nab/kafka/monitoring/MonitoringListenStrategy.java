package ru.hh.nab.kafka.monitoring;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ListenStrategy;
import ru.hh.nab.metrics.StatsDSender;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MonitoringListenStrategy<T> implements ListenStrategy<T> {

  private final StatsDSender statsDSender;
  private final ListenStrategy<T> listenStrategy;

  public MonitoringListenStrategy(StatsDSender statsDSender, ListenStrategy<T> listenStrategy) {
    this.statsDSender = statsDSender;
    this.listenStrategy = listenStrategy;
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack ack) {
    long start = System.nanoTime();
    listenStrategy.onMessagesBatch(messages, ack);
    statsDSender.sendTime(
        "batch-processing-time-ms",
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
    );
  }
}
