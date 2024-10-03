package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.producer.KafkaProducer;

public interface ConsumerBuilder<T> {

  ConsumerBuilder<T> withClientId(String clientId);

  ConsumerBuilder<T> withOperationName(String operationName);

  ConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy);

  /**
   * Enable retries for this consumer.
   * <p>
   * To schedule message for retry use {@link Ack#retry(ConsumerRecord, Throwable)}. Later the message with exactly the same key and value
   * will be supplied to the same {@link ConsumeStrategy}. Its headers will be also preserved and message processing history
   * will be added to special header. It can be obtained by using {@link HeadersMessageMetadataProvider#getMessageProcessingHistory(Headers)}
   * <p>
   * Retries are implemented as sending failed messages to special Kafka topic <i>topicName_operationName_retry_send</i>.
   * Retry implementation also starts another consumer (named <i>retry consumer</i>) which consumes messages
   * from topic <i>topicName_operationName_retry_receive</i> and supplies them to the same {@link ConsumeStrategy} as main consumer.
   * Be careful: this means {@link ConsumeStrategy} SHOULD BE thread-safe.
   * <p>
   * This scheme can only work with the help of another service that consumes messages from <i>topicName_operationName_retry_send</i>
   * and puts them into <i>topicName_operationName_retry_receive</i>. This can be simplified if you don't use progressive retries,
   * that is, each failed message is either retried after fixed duration (say, 10 seconds) or dropped. If this is your case
   * you may specify {@code useSingleRetryTopic=true} to send failed messages directly to <i>topicName_operationName_retry_receive</i> topic.
   * Retry consumer is already configured to consume retry messages only when they are ready for processing.
   */
  ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver, boolean useSingleRetryTopic);

  ConsumerBuilder<T> withLogger(Logger logger);

  ConsumerBuilder<T> withAckProvider(AckProvider<T> ackProvider);

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

  KafkaConsumer<T> start();
}
