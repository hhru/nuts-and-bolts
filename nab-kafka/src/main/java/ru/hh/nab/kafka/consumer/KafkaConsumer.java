package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

public class KafkaConsumer<T> {

  private final AbstractMessageListenerContainer<String, T> springKafkaContainer;

  private final ThreadLocal<List<ConsumerRecord<String, T>>> currentBatch = new InheritableThreadLocal<>();
  private final ThreadLocal<ConsumerRecord<String, T>> lastAckedBatchRecord = new InheritableThreadLocal<>();
  private final ConsumeStrategy<T> consumeStrategy;

  public KafkaConsumer(AbstractMessageListenerContainer<String, T> springKafkaContainer, ConsumeStrategy<T> consumeStrategy) {
    this.springKafkaContainer = springKafkaContainer;
    this.consumeStrategy = consumeStrategy;
  }

  void start() {
    springKafkaContainer.start();
  }

  void stop(Runnable callback) {
    springKafkaContainer.stop(callback);
  }

  void stop() {
    springKafkaContainer.stop();
  }

  public Collection<TopicPartition> getAssignedPartitions() {
    return springKafkaContainer.getAssignedPartitions();
  }

  public ConsumerRecord<String, T> getLastAckedBatchRecord() {
    return lastAckedBatchRecord.get();
  }

  public List<ConsumerRecord<String, T>> getCurrentBatch() {
    return currentBatch.get();
  }

  public void setLastAckedBatchRecord(ConsumerRecord<String, T> record) {
    lastAckedBatchRecord.set(record);
  }

  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer) {
    List<ConsumerRecord<String, T>> sortedBatch = messages.stream().sorted(
        Comparator.comparing((Function<ConsumerRecord<String, T>, String>) ConsumerRecord::topic)
            .thenComparingInt(ConsumerRecord::partition)
            .thenComparingLong(ConsumerRecord::offset)
    ).collect(Collectors.toUnmodifiableList());

    lastAckedBatchRecord.remove();
    currentBatch.set(sortedBatch);
    Ack<T> ack = new KafkaInternalTopicAck<>(this, consumer);
    consumeStrategy.onMessagesBatch(sortedBatch, ack);
  }
}
