package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import ru.hh.nab.kafka.util.AckUtils;

class KafkaInternalTopicAck<T> implements Ack<T> {

  private final KafkaConsumer<T> kafkaConsumer;
  private final Consumer<?, ?> consumer;

  public KafkaInternalTopicAck(KafkaConsumer<T> kafkaConsumer, Consumer<?, ?> consumer) {
    this.kafkaConsumer = kafkaConsumer;
    this.consumer = consumer;
  }

  @Override
  public void acknowledge() {
    consumer.commitSync();
    kafkaConsumer.getConsumingState().setWholeBatchCommited(true);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = AckUtils.getOffsetOfNextMessage(message);
    consumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    kafkaConsumer.getConsumingState().seekOffset(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    ConsumerConsumingState<T> consumingState = kafkaConsumer.getConsumingState();
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = AckUtils.getLatestOffsetForEachPartition(messages);
    consumer.commitSync(latestOffsetsForEachPartition);
    latestOffsetsForEachPartition.forEach(consumingState::seekOffset);
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    consumer.commitSync(AckUtils.getLatestOffsetForEachPartition(messages));
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    kafkaConsumer.getConsumingState().seekOffset(partition, AckUtils.getOffsetOfNextMessage(message));
  }

}
