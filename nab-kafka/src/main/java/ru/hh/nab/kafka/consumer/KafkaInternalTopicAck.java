package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public class KafkaInternalTopicAck<T> implements Ack<T> {

  private final KafkaConsumer<T> kafkaConsumer;
  private final Consumer<?, ?> consumer;

  public KafkaInternalTopicAck(KafkaConsumer<T> kafkaConsumer,
                               Consumer<?, ?> consumer) {
    this.kafkaConsumer = kafkaConsumer;
    this.consumer = consumer;
  }

  @Override
  public void acknowledge() {
    consumer.commitSync();
    kafkaConsumer.setWholeBatchCommited(true);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    TopicPartition partition = getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = getOffsetOfNextMessage(message);
    consumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    seek(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = getLatestOffsetForEachPartition(messages);
    consumer.commitSync(latestOffsetsForEachPartition);
    seek(latestOffsetsForEachPartition);
  }

  @Override
  public void commitOnly(Collection<ConsumerRecord<String, T>> messages) {
    consumer.commitSync(getLatestOffsetForEachPartition(messages));
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    TopicPartition partition = getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = getOffsetOfNextMessage(message);
    seek(partition, offsetOfNextMessageInPartition);
  }

  private OffsetAndMetadata getOffsetOfNextMessage(ConsumerRecord<String, T> message) {
    return new OffsetAndMetadata(message.offset() + 1);
  }

  private TopicPartition getMessagePartition(ConsumerRecord<String, T> message) {
    return new TopicPartition(message.topic(), message.partition());
  }

  private void seek(TopicPartition topicPartition, OffsetAndMetadata offsetAndMetadata) {
    kafkaConsumer.getSeekedOffsets().put(topicPartition, offsetAndMetadata);
  }

  private void seek(Map<TopicPartition, OffsetAndMetadata> offsets) {
    kafkaConsumer.getSeekedOffsets().putAll(offsets);
  }

  private Map<TopicPartition, OffsetAndMetadata> getLatestOffsetForEachPartition(Collection<ConsumerRecord<String, T>> messages) {
    return messages.stream().collect(
            Collectors.toMap(
                    this::getMessagePartition,
                    this::getOffsetOfNextMessage,
                    BinaryOperator.maxBy(Comparator.comparingLong(OffsetAndMetadata::offset))
            )
    );
  }
}
