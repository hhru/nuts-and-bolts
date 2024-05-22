package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import ru.hh.nab.kafka.util.AckUtils;

class KafkaInternalTopicAck<T> implements Ack<T> {

  private final ConsumerConsumingState<T> consumingState;
  private final Consumer<?, ?> nativeKafkaConsumer;

  public KafkaInternalTopicAck(KafkaConsumer<T> kafkaConsumer, Consumer<?, ?> nativeKafkaConsumer) {
    this.consumingState = kafkaConsumer.getConsumingState();
    this.nativeKafkaConsumer = nativeKafkaConsumer;
  }

  @Override
  public void acknowledge() {
    nativeKafkaConsumer.commitSync();
    consumingState.setWholeBatchCommited(true);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = AckUtils.getOffsetOfNextMessage(message);
    nativeKafkaConsumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    consumingState.seekOffset(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = AckUtils.getLatestOffsetForEachPartition(messages);
    nativeKafkaConsumer.commitSync(latestOffsetsForEachPartition);
    latestOffsetsForEachPartition.forEach(consumingState::seekOffset);
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    nativeKafkaConsumer.commitSync(AckUtils.getLatestOffsetForEachPartition(messages));
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    consumingState.seekOffset(partition, AckUtils.getOffsetOfNextMessage(message));
  }

}
