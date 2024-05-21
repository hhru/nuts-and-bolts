package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public class ConsumerConsumingState<T> {

  private final ThreadLocal<List<ConsumerRecord<String, T>>> currentBatch = new InheritableThreadLocal<>();
  private final ThreadLocal<Map<TopicPartition, OffsetAndMetadata>> batchSeekedOffsets = new InheritableThreadLocal<>();
  private final Map<TopicPartition, OffsetAndMetadata> globalSeekedOffsets;
  private final ThreadLocal<Boolean> wholeBatchCommited = new InheritableThreadLocal<>() {
    @Override
    protected Boolean initialValue() {
      return Boolean.FALSE;
    }
  };

  public ConsumerConsumingState() {
    this.globalSeekedOffsets = new ConcurrentHashMap<>();
  }

  public List<ConsumerRecord<String, T>> getCurrentBatch() {
    return currentBatch.get();
  }

  public void prepareForNextBatch(List<ConsumerRecord<String, T>> batchMessages) {
    batchSeekedOffsets.set(new ConcurrentHashMap<>());
    wholeBatchCommited.set(false);
    currentBatch.set(batchMessages);
  }


  public void seekOffset(TopicPartition topic, OffsetAndMetadata offset) {
    batchSeekedOffsets.get().put(topic, offset);
    globalSeekedOffsets.put(topic, offset);
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
