package ru.hh.nab.kafka.consumer;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
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
 * Class represents retries capabilities for a kafka message
 */
class RetryService<T> {

  protected final KafkaProducer dlqProducer;

  protected final RetryPolicyResolver<T> retryPolicyResolver;
  protected final KafkaProducer retryProducer;
  protected final RetryTopics retryTopics;
  protected final String deadLetterQueueDestination;
  protected final Clock clock;

  public RetryService(
      KafkaProducer dlqProducer,
      KafkaProducer retryProducer,
      RetryTopics retryTopics,
      RetryPolicyResolver<T> retryPolicyResolver,
      String deadLetterQueueDestination
  ) {
    this(dlqProducer, retryProducer, retryTopics, retryPolicyResolver, deadLetterQueueDestination, Clock.systemDefaultZone());
  }

  RetryService(
      KafkaProducer dlqProducer,
      KafkaProducer retryProducer,
      RetryTopics retryTopics,
      RetryPolicyResolver<T> retryPolicyResolver,
      String deadLetterQueueDestination,
      Clock clock
  ) {
    this.dlqProducer = dlqProducer;
    this.retryProducer = Objects.requireNonNull(retryProducer);
    this.retryTopics = Objects.requireNonNull(retryTopics);
    this.retryPolicyResolver = Objects.requireNonNull(retryPolicyResolver);
    this.deadLetterQueueDestination = deadLetterQueueDestination;
    this.clock = clock;
  }

  CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    RetryPolicy retryPolicy = retryPolicyResolver.apply(message, error);

    MessageProcessingHistory updatedProcessingHistory = getMessageProcessingHistory(message.headers())
        .map(messageProcessingHistory -> messageProcessingHistory.withOneMoreFail(Instant.now(clock)))
        .orElseGet(() -> MessageProcessingHistory.initial(Instant.ofEpochMilli(message.timestamp()), Instant.now(clock)));

    CompletableFuture<?> response = retryPolicy
        .getNextRetryTime(updatedProcessingHistory)
        .map(nextRetryTime -> retry(message, nextRetryTime, updatedProcessingHistory))
        // Either retry budged exhausted or it was configured to never retry ( second case handled above )
        .orElseGet(() -> {
          ProducerRecord<String, T> record = new ProducerRecord<>(deadLetterQueueDestination, null, message.key(), message.value());
          try {
            dlqProducer.sendMessage(record, Runnable::run);
          } catch (KafkaException e) {
            // TODO authentication, timeouts, overload, other errors ?
          }
          return CompletableFuture.completedFuture(null);
        });
    return response;
  }

  protected CompletableFuture<?> retry(ConsumerRecord<String, T> message, Instant retryTime, MessageProcessingHistory updatedProcessingHistory) {
    ProducerRecord<String, T> retryRecord = new ProducerRecord<>(retryTopics.retrySendTopic(), null, message.key(), message.value());
    setMessageProcessingHistory(retryRecord.headers(), updatedProcessingHistory);
    setNextRetryTime(retryRecord.headers(), retryTime);
    if (!retryTopics.isSingleTopic()) {
      setRetryReceiveTopic(retryRecord.headers(), retryTopics);
    }
    return retryProducer.sendMessage(retryRecord, Runnable::run);
  }
}
