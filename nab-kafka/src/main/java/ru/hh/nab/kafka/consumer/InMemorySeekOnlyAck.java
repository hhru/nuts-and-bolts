package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import ru.hh.nab.kafka.util.AckUtils;

class InMemorySeekOnlyAck<T> implements Ack<T> {

  private final ConsumerConsumingState<T> consumingState;
  private final DeadLetterQueue<T> deadLetterQueue;

  public InMemorySeekOnlyAck(KafkaConsumer<T> kafkaConsumer) {
    this.consumingState = kafkaConsumer.getConsumingState();
    this.deadLetterQueue = kafkaConsumer.getDeadLetterQueue();
  }

  @Override
  public void acknowledge() {
    consumingState.setWholeBatchCommited(true);
    acknowledge(consumingState.getCurrentBatch());
  }

  @Override
  public void nAcknowledge() {
    consumingState.setWholeBatchCommited(true);
    nAcknowledge(consumingState.getCurrentBatch());
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    seek(message);
  }

  @Override
  public void nAcknowledge(ConsumerRecord<String, T> message) {
    deadLetterQueue.send(message);
    seek(message);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> offsets = AckUtils.getLatestOffsetForEachPartition(messages);
    offsets.forEach((partition, offset) -> consumingState.seekOffset(partition, offset));
  }

  @Override
  public void nAcknowledge(Collection<ConsumerRecord<String, T>> messages) {
    for (ConsumerRecord<String, T> record : messages) {
      deadLetterQueue.send(record);
    }
    Map<TopicPartition, OffsetAndMetadata> offsets = AckUtils.getLatestOffsetForEachPartition(messages);
    offsets.forEach(consumingState::seekOffset);
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    throw new UnsupportedOperationException("commit is not supported for InMemorySeekOnlyAck");
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    consumingState.seekOffset(AckUtils.getMessagePartition(message), AckUtils.getOffsetOfNextMessage(message));
  }

  @Override
  public CompletableFuture<Void> retry(ConsumerRecord<String, T> message, Throwable error) {
    throw new UnsupportedOperationException("Retry is not supported by InMemorySeekOnlyAck");
  }
}
