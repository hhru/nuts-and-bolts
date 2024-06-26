package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;

public interface ConsumerBuilder<T> {

  ConsumerBuilder<T> withClientId(String clientId);

  ConsumerBuilder<T> withOperationName(String operationName);

  ConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy);

  ConsumerBuilder<T> withLogger(Logger logger);

  ConsumerBuilder<T> withAckProvider(BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider);

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
