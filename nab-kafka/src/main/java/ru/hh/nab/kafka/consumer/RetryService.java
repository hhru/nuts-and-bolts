package ru.hh.nab.kafka.consumer;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setNextRetryTime;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setRetryReceiveTopic;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;

/**
 * Provides retries implementation to {@link ru.hh.nab.kafka.consumer.Ack}.
 */
class RetryService<T> {

  protected final RetryPolicyResolver<T> retryPolicyResolver;
  protected final KafkaProducer retryProducer;
  protected final RetryTopics retryTopics;
  protected final Clock clock;

  public RetryService(KafkaProducer retryProducer, RetryTopics retryTopics, RetryPolicyResolver<T> retryPolicyResolver) {
    this(retryProducer, retryTopics, retryPolicyResolver, Clock.systemDefaultZone());
  }

  RetryService(KafkaProducer retryProducer, RetryTopics retryTopics, RetryPolicyResolver<T> retryPolicyResolver, Clock clock) {
    this.retryProducer = Objects.requireNonNull(retryProducer);
    this.retryTopics = Objects.requireNonNull(retryTopics);
    this.retryPolicyResolver = Objects.requireNonNull(retryPolicyResolver);
    this.clock = clock;
  }

  CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    RetryPolicy retryPolicy = retryPolicyResolver.apply(message, error);
    MessageProcessingHistory updatedProcessingHistory = getMessageProcessingHistory(message.headers())
        .map(messageProcessingHistory -> messageProcessingHistory.withOneMoreFail(Instant.now(clock)))
        .orElseGet(() -> MessageProcessingHistory.initial(Instant.ofEpochMilli(message.timestamp()), Instant.now(clock)));
    return retryPolicy
        .getNextRetryTime(updatedProcessingHistory)
        .map(nextRetryTime -> retry(message, nextRetryTime, updatedProcessingHistory))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  protected CompletableFuture<?> retry(ConsumerRecord<String, T> message, Instant retryTime, MessageProcessingHistory updatedProcessingHistory) {
    ProducerRecord<String, T> retryRecord = new ProducerRecord<>(retryTopics.retrySendTopic(), null,  message.key(), message.value());
    setMessageProcessingHistory(retryRecord.headers(), updatedProcessingHistory);
    setNextRetryTime(retryRecord.headers(), retryTime);
    if (!retryTopics.isSingleTopic()) {
      setRetryReceiveTopic(retryRecord.headers(), retryTopics);
    }
    return retryProducer.sendMessage(retryRecord, Runnable::run);
  }
}
