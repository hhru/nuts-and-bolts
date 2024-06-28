package ru.hh.nab.kafka.consumer.retry;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.Ack;

@FunctionalInterface
public interface RetryingConsumeStrategy<T> {

  void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack, RetryService<T> retryService) throws InterruptedException;
}
