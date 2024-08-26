package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface MessageMetadataProvider {
  Optional<MessageProcessingHistory> getMessageProcessingHistory(ConsumerRecord<?, ?> consumerRecord);

  void setMessageProcessingHistory(ProducerRecord<?, ?> producerRecord, MessageProcessingHistory messageProcessingHistory);

  Optional<Instant> getNextRetryTime(ConsumerRecord<?, ?> consumerRecord);

  void setNextRetryTime(ProducerRecord<?, ?> producerRecord, Instant nextRetryTime);
}
