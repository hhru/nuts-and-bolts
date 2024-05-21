package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import ru.hh.nab.kafka.util.AckUtils;

class InMemorySeekOnlyAck<T> implements Ack<T> {

  private final KafkaConsumer<T> kafkaConsumer;

  public InMemorySeekOnlyAck(KafkaConsumer<T> kafkaConsumer) {
    this.kafkaConsumer = kafkaConsumer;
  }

  @Override
  public void acknowledge() {
    ConsumerConsumingState<T> consumingState = kafkaConsumer.getConsumingState();
    consumingState.setWholeBatchCommited(true);
    acknowledge(consumingState.getCurrentBatch());
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    seek(message);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    ConsumerConsumingState<T> consumingState = kafkaConsumer.getConsumingState();
    Map<TopicPartition, OffsetAndMetadata> offsets = AckUtils.getLatestOffsetForEachPartition(messages);
    offsets.forEach((partition, offset) -> consumingState.seekOffset(partition, offset));
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    // commit is not performed, because offset is stored in memory only;
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    kafkaConsumer.getConsumingState().seekOffset(AckUtils.getMessagePartition(message), AckUtils.getOffsetOfNextMessage(message));
  }

}
