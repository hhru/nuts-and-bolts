package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class SimpleDelayedConsumeStrategy<T> implements ConsumeStrategy<T> {
  private final ConsumeStrategy<T> delegate;
  private final Function<ConsumerRecord<String, T>, Instant> getReadyTime;
  private final Duration sleepIfNotReadyDuration;

  public SimpleDelayedConsumeStrategy(
      ConsumeStrategy<T> delegate,
      Function<ConsumerRecord<String, T>, Instant> getReadyTime,
      Duration sleepIfNotReadyDuration
  ) {
    this.delegate = delegate;
    this.getReadyTime = getReadyTime;
    this.sleepIfNotReadyDuration = sleepIfNotReadyDuration;
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException {
    List<ConsumerRecord<String, T>> readyMessages = messages
        .stream()
        .filter(message -> getReadyTime.apply(message).isBefore(Instant.now()))
        .toList();
    if (readyMessages.isEmpty()) {
      Thread.sleep(sleepIfNotReadyDuration.toMillis());
      return;
    }
    delegate.onMessagesBatch(readyMessages, new PartialAck<>(ack, readyMessages));
  }
}
