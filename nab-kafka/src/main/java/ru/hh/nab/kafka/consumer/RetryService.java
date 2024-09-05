package ru.hh.nab.kafka.consumer;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;


/**
 * Provides retries implementation and message processing history to {@link ru.hh.nab.kafka.consumer.Ack}.
 */
class RetryService<T> {

  protected final RetryPolicyResolver<T> retryPolicyResolver;
  protected final HeadersMessageMetadataProvider messageMetadataProvider = new HeadersMessageMetadataProvider();
  protected final KafkaProducer retryProducer;
  protected final String retryTopic;

  public RetryService(KafkaProducer retryProducer, String retryTopic, RetryPolicyResolver<T> retryPolicyResolver) {
    this.retryPolicyResolver = retryPolicyResolver;
    this.retryProducer = Objects.requireNonNull(retryProducer);
    this.retryTopic = Objects.requireNonNull(retryTopic);
  }

  public CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    RetryPolicy retryPolicy = getRetryPolicy(message, error);
    MessageProcessingHistory updatedProcessingHistory = getProcessingHistory(message)
        .map(MessageProcessingHistory::withOneMoreFail)
        .orElseGet(() -> createInitialProcessingHistory(message));
    return retryPolicy
        .getNextRetryTime(updatedProcessingHistory)
        .map(nextRetryTime -> retry(message, nextRetryTime, updatedProcessingHistory))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  protected CompletableFuture<?> retry(ConsumerRecord<String, T> message, Instant retryTime, MessageProcessingHistory updatedProcessingHistory) {
    ProducerRecord<String, T> retryRecord = new ProducerRecord<>(retryTopic, null,  message.key(), message.value(), new RecordHeaders());
    messageMetadataProvider.setMessageProcessingHistory(retryRecord, updatedProcessingHistory);
    messageMetadataProvider.setNextRetryTime(retryRecord, retryTime);
    return retryProducer.sendMessage(retryRecord, Runnable::run);
  }

  public Optional<MessageProcessingHistory> getProcessingHistory(ConsumerRecord<String, T> message) {
    return messageMetadataProvider.getMessageProcessingHistory(message);
  }

  protected MessageProcessingHistory createInitialProcessingHistory(ConsumerRecord<String, T> message) {
    return new MessageProcessingHistory(Instant.ofEpochMilli(message.timestamp()), 1, Instant.now());
  }

  protected RetryPolicy getRetryPolicy(ConsumerRecord<String, T> message, Throwable error) {
    return retryPolicyResolver.apply(message, error);
  }
}
