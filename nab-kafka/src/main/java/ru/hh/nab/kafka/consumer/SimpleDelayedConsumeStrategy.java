package ru.hh.nab.kafka.consumer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class SimpleDelayedConsumeStrategy<T> implements ConsumeStrategy<T> {
  private final ConsumeStrategy<T> delegate;
  private final Function<ConsumerRecord<String, T>, Instant> getReadyTime;
  private final Duration sleepIfNotReadyDuration;
  private final Clock clock;

  public SimpleDelayedConsumeStrategy(
      ConsumeStrategy<T> delegate,
      Function<ConsumerRecord<String, T>, Instant> getReadyTime,
      Duration sleepIfNotReadyDuration
  ) {
    this(delegate, getReadyTime, sleepIfNotReadyDuration, Clock.systemDefaultZone());
  }

  SimpleDelayedConsumeStrategy(
      ConsumeStrategy<T> delegate,
      Function<ConsumerRecord<String, T>, Instant> getReadyTime,
      Duration sleepIfNotReadyDuration,
      Clock clock
  ) {
    this.delegate = delegate;
    this.getReadyTime = getReadyTime;
    this.sleepIfNotReadyDuration = sleepIfNotReadyDuration;
    this.clock = clock;
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException {
    List<ConsumerRecord<String, T>> readyMessages = messages
        .stream()
        .filter(message -> Instant.now(clock).isAfter(getReadyTime.apply(message)))
        .toList();
    if (readyMessages.isEmpty()) {
      Thread.sleep(sleepIfNotReadyDuration.toMillis());
      return;
    }
    delegate.onMessagesBatch(readyMessages, new PartialAck<>(ack, readyMessages));
  }
}
