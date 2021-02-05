package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

public class KafkaConsumer<T> {

  private final AbstractMessageListenerContainer<String, T> springKafkaContainer;

  private final ThreadLocal<List<ConsumerRecord<String, T>>> currentBatch = new InheritableThreadLocal<>();
  private final ThreadLocal<Map<TopicPartition, OffsetAndMetadata>> seekedOffsets = new InheritableThreadLocal<>();
  private final ThreadLocal<Boolean> wholeBatchCommited = new InheritableThreadLocal<>();

  private final ConsumeStrategy<T> consumeStrategy;

  public KafkaConsumer(ConsumeStrategy<T> consumeStrategy,
                       Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider) {
    this.consumeStrategy = consumeStrategy;
    this.springKafkaContainer = springContainerProvider.apply(this);
  }

  public void start() {
    springKafkaContainer.start();
  }

  public void stop(Runnable callback) {
    springKafkaContainer.stop(callback);
  }

  public void stop() {
    springKafkaContainer.stop();
  }

  public Collection<TopicPartition> getAssignedPartitions() {
    return springKafkaContainer.getAssignedPartitions();
  }

  public Map<TopicPartition, OffsetAndMetadata> getSeekedOffsets() {
    return seekedOffsets.get();
  }

  public void setWholeBatchCommited(boolean wholeBatchCommitedStatus) {
    wholeBatchCommited.set(wholeBatchCommitedStatus);
  }

  public List<ConsumerRecord<String, T>> getCurrentBatch() {
    return currentBatch.get();
  }

  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer) {
    seekedOffsets.set(new ConcurrentHashMap<>());
    wholeBatchCommited.set(false);
    currentBatch.set(messages);

    Ack<T> ack = new KafkaInternalTopicAck<>(this, consumer);
    processMessages(messages, ack);
    rewindToLastAckedOffset(consumer);
  }

  private void processMessages(List<ConsumerRecord<String, T>> messages, Ack<T> ack) {
    try {
      consumeStrategy.onMessagesBatch(messages, ack);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted thread during kafka processing", e);
    }
  }

  public void rewindToLastAckedOffset(Consumer<?, ?> consumer) {
    if (wholeBatchCommited.get()) {
      return;
    }

    List<ConsumerRecord<String, T>> messages = getCurrentBatch();
    if (messages.isEmpty()) {
      return;
    }

    LinkedHashMap<TopicPartition, OffsetAndMetadata> offsetsToSeek = getLowestOffsetsForEachPartition(messages);
    getSeekedOffsets().forEach(offsetsToSeek::put);
    offsetsToSeek.forEach(consumer::seek);
  }

  private LinkedHashMap<TopicPartition, OffsetAndMetadata> getLowestOffsetsForEachPartition(List<ConsumerRecord<String, T>> messages) {
    return messages.stream().collect(toMap(
        record -> new TopicPartition(record.topic(), record.partition()),
        record -> new OffsetAndMetadata(record.offset()),
        BinaryOperator.minBy(Comparator.comparing(OffsetAndMetadata::offset)),
        LinkedHashMap::new
    ));
  }
}
