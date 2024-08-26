package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.hh.nab.kafka.producer.KafkaProducer;

public abstract sealed class KafkaRetryService<T> extends RetryService<T> permits KafkaFixedDelayRetryService, KafkaProgressiveRetryService {
  private final MessageMetadataProvider messageMetadataProvider;
  private final KafkaProducer retryProducer;
  private final String retryTopic;

  public KafkaRetryService(KafkaProducer retryProducer, String retryTopic) {
    this(retryProducer, retryTopic, new HeadersMessageMetadataProvider());
  }

  public KafkaRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      MessageMetadataProvider messageMetadataProvider
  ) {
    this.messageMetadataProvider = messageMetadataProvider;
    this.retryProducer = retryProducer;
    this.retryTopic = retryTopic;
  }

  @Override
  protected CompletableFuture<?> retry(ConsumerRecord<String, T> message, Instant retryTime, MessageProcessingHistory updatedProcessingHistory) {
    ProducerRecord<String, T> retryRecord = new ProducerRecord<>(retryTopic, message.key(), message.value());
    messageMetadataProvider.setMessageProcessingHistory(retryRecord, updatedProcessingHistory);
    messageMetadataProvider.setNextRetryTime(retryRecord, retryTime);
    return retryProducer.sendMessage(retryTopic, retryRecord);
  }

  @Override
  public Optional<MessageProcessingHistory> getProcessingHistory(ConsumerRecord<String, T> message) {
    return messageMetadataProvider.getMessageProcessingHistory(message);
  }
}
