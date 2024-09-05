package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.util.AckUtils;

class KafkaInternalTopicAck<T> implements Ack<T> {

  private final ConsumerConsumingState<T> consumingState;
  private final Consumer<?, ?> nativeKafkaConsumer;
  private final RetryService<T> retryService;

  public KafkaInternalTopicAck(
      KafkaConsumer<T> kafkaConsumer,
      Consumer<?, ?> nativeKafkaConsumer,
      RetryService<T> retryService) {
    this.consumingState = kafkaConsumer.getConsumingState();
    this.nativeKafkaConsumer = nativeKafkaConsumer;
    this.retryService = retryService;
  }

  @Override
  public void acknowledge() {
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync();
    consumingState.setWholeBatchCommited(true);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = AckUtils.getOffsetOfNextMessage(message);
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    consumingState.seekOffset(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = AckUtils.getLatestOffsetForEachPartition(messages);
    waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(latestOffsetsForEachPartition);
    latestOffsetsForEachPartition.forEach(consumingState::seekOffset);
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    //TODO Непонятно, надо ли здесь waitForRetriesToComplete();
    nativeKafkaConsumer.commitSync(AckUtils.getLatestOffsetForEachPartition(messages));
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    waitForRetriesToComplete();
    consumingState.seekOffset(partition, AckUtils.getOffsetOfNextMessage(message));
  }

  @Override
  public CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    if (retryService == null) {
      throw new UnsupportedOperationException("This Consumer has not been configured for retries");
    }
    CompletableFuture<?> retryFuture = retryService.retry(message, error);
    consumingState.addRetryFuture(retryFuture);
    return retryFuture;
  }

  @Override
  public Optional<MessageProcessingHistory> getProcessingHistory(ConsumerRecord<String, T> message) {
    if (retryService == null) {
      throw new UnsupportedOperationException(
          "This Consumer has not been configured for retries so there is no meaningful processing history");
    }
    return retryService.getProcessingHistory(message);
  }

  private void waitForRetriesToComplete() {
    try {
      consumingState.getAllBatchRetryFuturesAsOne().get();
    } catch (InterruptedException e) {
      throw new InterruptException(e);
    } catch (ExecutionException | CancellationException e) {
      throw e.getCause() instanceof KafkaException ? (KafkaException) e.getCause() : new KafkaException(e);
    }
  }
}
