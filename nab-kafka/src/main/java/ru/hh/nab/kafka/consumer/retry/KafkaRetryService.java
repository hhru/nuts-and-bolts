package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
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
    this.messageMetadataProvider = Objects.requireNonNull(messageMetadataProvider);
    this.retryProducer = Objects.requireNonNull(retryProducer);
    this.retryTopic = Objects.requireNonNull(retryTopic);
  }

  @Override
  protected CompletableFuture<?> retry(ConsumerRecord<String, T> message, Instant retryTime, MessageProcessingHistory updatedProcessingHistory) {
    ProducerRecord<String, T> retryRecord = new ProducerRecord<>(retryTopic, null,  message.key(), message.value(), new RecordHeaders());
    messageMetadataProvider.setMessageProcessingHistory(retryRecord, updatedProcessingHistory);
    messageMetadataProvider.setNextRetryTime(retryRecord, retryTime);
    return retryProducer.sendMessage(retryRecord, Runnable::run);
  }

  @Override
  public Optional<MessageProcessingHistory> getProcessingHistory(ConsumerRecord<String, T> message) {
    return messageMetadataProvider.getMessageProcessingHistory(message);
  }
}
