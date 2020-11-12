package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

public class KafkaConsumer<T> {

  private final AbstractMessageListenerContainer<String, T> springKafkaContainer;

  private final ThreadLocal<List<ConsumerRecord<String, T>>> currentBatch = new InheritableThreadLocal<>();
  private final ThreadLocal<ConsumerRecord<String, T>> lastAckedBatchRecord = new InheritableThreadLocal<>();
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
    if (!ack.isAcknowledge()) {
      rewindToLastAckedOffset(consumer);
    }
  }

  public void rewindToLastAckedOffset(Consumer<?, ?> consumer) {
    List<ConsumerRecord<String, T>> currentBatch = getCurrentBatch();
    if (!currentBatch.isEmpty()) {
      LinkedHashMap<TopicPartition, OffsetAndMetadata> offsetsToSeek = currentBatch.stream().collect(toMap(
          record -> new TopicPartition(record.topic(), record.partition()),
          record -> new OffsetAndMetadata(record.offset()),
          (offset1, offset2) -> offset1,
          LinkedHashMap::new
      ));

      Optional.ofNullable(getLastAckedBatchRecord()).ifPresent(lastAckedBatchRecord -> {
        for (ConsumerRecord<String, T> record : getCurrentBatch()) {
          offsetsToSeek.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
          if (record == lastAckedBatchRecord) {
            break;
          }
        }
      });

      offsetsToSeek.forEach(consumer::seek);
    }
  }
}
