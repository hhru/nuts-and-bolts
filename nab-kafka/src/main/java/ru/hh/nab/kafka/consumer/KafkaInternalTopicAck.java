package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import ru.hh.nab.kafka.util.AckUtils;

class KafkaInternalTopicAck<T> implements Ack<T> {

  private final ConsumerContext<T> consumerContext;
  private final Consumer<?, ?> nativeKafkaConsumer;
  private final RetryQueue<T> retryQueue;
  private final DeadLetterQueue<T> deadLetterQueue;

  public KafkaInternalTopicAck(
      KafkaConsumer<T> kafkaConsumer,
      Consumer<?, ?> nativeKafkaConsumer
  ) {
    this.deadLetterQueue = kafkaConsumer.getDeadLetterQueue();
    this.consumerContext = kafkaConsumer.getConsumerContext();
    this.nativeKafkaConsumer = nativeKafkaConsumer;
    this.retryQueue = kafkaConsumer.getRetryQueue();
  }

  @Override
  public void acknowledge() {
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync();
    consumerContext.setWholeBatchCommited(true);
  }

  @Override
  public void nAcknowledge() {
    for (ConsumerRecord<String, T> record : consumerContext.getCurrentBatch()) {
      deadLetterQueue.send(record);
    }
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync();
    consumerContext.setWholeBatchCommited(true);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = AckUtils.getOffsetOfNextMessage(message);
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    consumerContext.seekOffset(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void nAcknowledge(ConsumerRecord<String, T> message) {
    deadLetterQueue.send(message);

    TopicPartition partition = AckUtils.getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = AckUtils.getOffsetOfNextMessage(message);
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    consumerContext.seekOffset(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = AckUtils.getLatestOffsetForEachPartition(messages);
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(latestOffsetsForEachPartition);
    latestOffsetsForEachPartition.forEach(consumerContext::seekOffset);
  }

  @Override
  public void nAcknowledge(Collection<ConsumerRecord<String, T>> messages) {
    for (ConsumerRecord<String, T> record : messages) {
      deadLetterQueue.send(record);
    }
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = AckUtils.getLatestOffsetForEachPartition(messages);
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(latestOffsetsForEachPartition);
    latestOffsetsForEachPartition.forEach(consumerContext::seekOffset);
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(AckUtils.getLatestOffsetForEachPartition(messages));
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    waitForRetriesToComplete();
    consumerContext.seekOffset(partition, AckUtils.getOffsetOfNextMessage(message));
  }

  @Override
  public CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    if (retryQueue == null) {
      throw new UnsupportedOperationException("This Consumer has not been configured for retries");
    }
    CompletableFuture<?> retryFuture = retryQueue.retry(message, error);
    consumerContext.addRetryFuture(retryFuture, message);
    return retryFuture;
  }

  private void waitForRetriesToComplete() {
    try {
      consumerContext.getAllBatchRetryFuturesAsOne().get();
      AckUtils.getLatestOffsetForEachPartition(consumerContext.getBatchRetryMessages()).forEach(consumerContext::seekOffset);
    } catch (InterruptedException e) {
      throw new InterruptException(e);
    } catch (ExecutionException | CancellationException e) {
      throw e.getCause() instanceof KafkaException ? (KafkaException) e.getCause() : new KafkaException(e);
    }
  }
}
