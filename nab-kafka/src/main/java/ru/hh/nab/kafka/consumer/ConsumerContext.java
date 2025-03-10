package ru.hh.nab.kafka.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

class ConsumerContext<T> {

  private final ThreadLocal<List<ConsumerRecord<String, T>>> currentBatch = new InheritableThreadLocal<>();
  private final ThreadLocal<Map<TopicPartition, OffsetAndMetadata>> batchSeekedOffsets = new InheritableThreadLocal<>();
  private final Map<TopicPartition, OffsetAndMetadata> globalSeekedOffsets;
  private final ThreadLocal<List<CompletableFuture<?>>> batchFutures = new InheritableThreadLocal<>();
  private final ThreadLocal<List<ConsumerRecord<String, T>>> batchFutureMessages = new InheritableThreadLocal<>();
  private final ThreadLocal<Boolean> wholeBatchCommited = new InheritableThreadLocal<>() {
    @Override
    protected Boolean initialValue() {
      return Boolean.FALSE;
    }
  };

  public ConsumerContext() {
    this.globalSeekedOffsets = new ConcurrentHashMap<>();
  }

  public List<ConsumerRecord<String, T>> getCurrentBatch() {
    return currentBatch.get();
  }

  public void prepareForNextBatch(List<ConsumerRecord<String, T>> batchMessages) {
    batchSeekedOffsets.set(new ConcurrentHashMap<>());
    batchFutures.set(new ArrayList<>());
    batchFutureMessages.set(new ArrayList<>());
    wholeBatchCommited.set(false);
    currentBatch.set(batchMessages);
  }

  public void seekOffset(TopicPartition topic, OffsetAndMetadata offset) {
    batchSeekedOffsets.get().put(topic, offset);
    globalSeekedOffsets.put(topic, offset);
  }

  public void addFutureMessage(CompletableFuture<?> future, ConsumerRecord<String, T> message) {
    batchFutures.get().add(future);
    batchFutureMessages.get().add(message);
  }

  public CompletableFuture<Void> getAllBatchFuturesAsOne() {
    return CompletableFuture.allOf(batchFutures.get().toArray(CompletableFuture<?>[]::new));
  }

  // Visible for testing
  List<CompletableFuture<?>> getBatchFutures() {
    return batchFutures.get();
  }

  public List<ConsumerRecord<String, T>> getBatchFutureMessages() {
    return batchFutureMessages.get();
  }

  public Optional<OffsetAndMetadata> getGlobalSeekedOffset(TopicPartition partition) {
    return Optional.ofNullable(globalSeekedOffsets.get(partition));
  }

  public boolean isWholeBatchAcked() {
    return wholeBatchCommited.get();
  }

  public void setWholeBatchCommited(boolean wholeBatchCommitedStatus) {
    wholeBatchCommited.set(wholeBatchCommitedStatus);
  }

  public Map<TopicPartition, OffsetAndMetadata> getBatchSeekedOffsets() {
    return batchSeekedOffsets.get();
  }

}
