package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;

public interface ConsumerBuilder<T> {

  ConsumerBuilder<T> withClientId(String clientId);

  ConsumerBuilder<T> withOperationName(String operationName);

  ConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy);

  /**
   * Configures kafka consumer for retries via {@link Ack#retry(ConsumerRecord, Throwable)}
   * <p>
   * Requirements : {@link ConsumeStrategy} MUST BE thread safe.
   * <p>
   * Messages scheduled for retries are stored in a separate kafka topic (see {@link RetryTopics#DEFAULT_SINGLE_TOPIC}).
   * Later retry messages will be read by a DIFFERENT kafka consumer and processed with the same {@link ConsumeStrategy}.
   *
   * @see #withRetries(KafkaProducer, RetryPolicyResolver, RetryTopics)
   * @see RetryPolicy
   * @see RetryPolicyResolver
   * @see HeadersMessageMetadataProvider
   * */
  default ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver) {
    return withRetries(retryProducer, retryPolicyResolver, RetryTopics.DEFAULT_SINGLE_TOPIC);
  }

  /**
   * Configures kafka consumer for retries via {@link Ack#retry(ConsumerRecord, Throwable)}
   * <p>
   * Requirements : {@link ConsumeStrategy} MUST BE thread safe.
   * <p>
   * Messages scheduled for retries are stored in a separate kafka topic ({@link RetryTopics#retrySendTopic()}).
   * Later retry messages will be read by a DIFFERENT kafka consumer and processed with the same {@link ConsumeStrategy}.
   * <p>
   * If <i>retryTopics</i> parameter specifies single topic then this service handles retries on its own.
   * This scheme requires scheduling messages for retry after fixed duration (see {@link RetryPolicy#fixed(Duration)}).
   * <p>
   * If <i>retryTopics</i> parameter specifies two different topics then another service is responsible
   * for scheduling next retry by sending message to {@link RetryTopics#retryReceiveTopic()}.
   * This scheme allows to schedule retry messages any way you want.
   *
   * @see #withRetryConsumeStrategy(ConsumeStrategy)
   * @see RetryPolicy
   * @see RetryPolicyResolver
   * @see RetryTopics
   * @see HeadersMessageMetadataProvider
   */
  ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver, RetryTopics retryTopics);

  /**
   * Specifies custom {@link ConsumeStrategy} for retries.
   * <p>
   * Requirements: custom strategy MUST consume retry messages only when they are ready for processing.
   * This can be done using {@link #decorateForDelayedRetry(ConsumeStrategy, Duration)}.
   * <p>
   * By default, retry consumer uses the same strategy as main and decorates it as mentioned above.
   * <p>
   * When your {@link ConsumeStrategy} is not thread-safe it might help to supply main and retry consumer
   * with two different instances of the same strategy class. You should remember that any common code (like singleton methods)
   * called from these two instances must be thread-safe.
   *
   * @see HeadersMessageMetadataProvider
   * */
  ConsumerBuilder<T> withRetryConsumeStrategy(ConsumeStrategy<T> retryConsumeStrategy);

  /**
   * Decorate consume strategy to process messages only when they are ready for retry.
   *
   * @see  HeadersMessageMetadataProvider#getNextRetryTime
   * @see SimpleDelayedConsumeStrategy
   * */
  static <T> SimpleDelayedConsumeStrategy<T> decorateForDelayedRetry(ConsumeStrategy<T> delegate, Duration sleepIfNotReadyDuration) {
    return new SimpleDelayedConsumeStrategy<>(
        delegate,
        message -> HeadersMessageMetadataProvider.getNextRetryTime(message.headers()).orElse(Instant.EPOCH),
        sleepIfNotReadyDuration
    );
  }

  ConsumerBuilder<T> withLogger(Logger logger);

  /**
   * Consumer будет включен в consumer-group: одновременно одна партиция топика не будет обрабатываться больше чем одним consumer-ом.
   * @return this
   */
  ConsumerBuilder<T> withConsumerGroup();

  /**
   * Consumer подпишется на все партиции, которые есть в топике.
   * Метод нужен в ситуациях, когда нужно не использовать consumer-group, а в каждом инстансе сервиса читать все партиции.
   * Обычно это нужно statetul-сервисам, которые поддерживают какое-то состояние внутри себя на основе сообщений из kafka.
   * @param seekPosition - Указывает откуда начинать читать сообщения - с начала и конца топика
   * @param checkNewPartitionsInterval - Как часто проверять наличие новых партиций в топике.
   *                                   Если null - проверяться не будут. По-умолчанию - 5 минут.
   * @return this
   */
  ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition, Duration checkNewPartitionsInterval);

  ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition);

  KafkaConsumer<T> build();

  /**
   * @deprecated Use {@link #build()} and then {@link KafkaConsumer#start()}
   * */
  @Deprecated(forRemoval = true)
  default KafkaConsumer<T> start() {
    KafkaConsumer<T> consumer = build();
    consumer.start();
    return consumer;
  }
}
