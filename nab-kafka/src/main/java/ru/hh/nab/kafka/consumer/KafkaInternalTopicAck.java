package ru.hh.nab.kafka.consumer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public class KafkaInternalTopicAck<T> implements Ack<T> {

  private final KafkaConsumer<T> kafkaConsumer;
  private final Consumer<?, ?> consumer;
  private Integer lastCommittedIndex = null;

  public KafkaInternalTopicAck(KafkaConsumer<T> kafkaConsumer,
                               Consumer<?, ?> consumer) {
    this.kafkaConsumer = kafkaConsumer;
    this.consumer = consumer;
  }

  @Override
  public void acknowledge() {
    List<ConsumerRecord<String, T>> currentBatch = kafkaConsumer.getCurrentBatch();
    if (!currentBatch.isEmpty()) {
      ConsumerRecord<String, T> lastRecord = currentBatch.get(currentBatch.size() - 1);
      acknowledge(lastRecord);
    }
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> processedMessage) {
    List<ConsumerRecord<String, T>> currentBatch = kafkaConsumer.getCurrentBatch();
    if (lastCommittedIndex != null && lastCommittedIndex >= currentBatch.size()) {
      return;
    }

    Integer startIndex = Optional.ofNullable(lastCommittedIndex).map(lci -> lci + 1).orElse(0);
    LinkedHashMap<TopicPartition, OffsetAndMetadata> offsetsToCommit = Stream.concat(
        currentBatch.subList(startIndex, currentBatch.size()).stream().takeWhile(message -> message != processedMessage),
        Stream.of(processedMessage)
    ).collect(Collectors.toMap(
        message -> new TopicPartition(message.topic(), message.partition()),
        message -> new OffsetAndMetadata(message.offset() + 1),
        (offset1, offset2) -> offset2,
        LinkedHashMap::new
    ));
    consumer.commitSync(offsetsToCommit);
    seek(processedMessage);
  }

  @Override
  public void seek(ConsumerRecord<String, T> processedMessage) {
    kafkaConsumer.setLastAckedBatchRecord(processedMessage);
  }
}
