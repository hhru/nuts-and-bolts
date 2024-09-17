package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;

class PartialAck<T> implements Ack<T> {
  private final Ack<T> delegate;
  private final Collection<ConsumerRecord<String, T>> messagesReadyForAcknowledge;

  public PartialAck(Ack<T> delegate, Collection<ConsumerRecord<String, T>> messagesReadyForAcknowledge) {
    this.delegate = delegate;
    this.messagesReadyForAcknowledge = messagesReadyForAcknowledge;
  }

  @Override
  public void acknowledge() {
    delegate.acknowledge(messagesReadyForAcknowledge);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    delegate.acknowledge(message);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    delegate.acknowledge(messages);
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    delegate.seek(message);
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    delegate.commit(messages);
  }

  @Override
  public CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    return delegate.retry(message, error);
  }
}
