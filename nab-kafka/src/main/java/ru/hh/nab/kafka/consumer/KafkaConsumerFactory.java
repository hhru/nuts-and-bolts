package ru.hh.nab.kafka.consumer;

import org.slf4j.Logger;

public interface KafkaConsumerFactory {

  /**
   * @deprecated Use {@link KafkaConsumerFactory#builder(String, Class) instead}
   */
  @Deprecated
  <T> KafkaConsumer<T> subscribe(
      String topicName,
      String operationName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumeStrategy
  );


  /**
   * @deprecated Use {@link KafkaConsumerFactory#builder(String, Class) instead}
   */
  @Deprecated
  <T> KafkaConsumer<T> subscribe(
      String topicName,
      String operationName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumeStrategy,
      Logger logger
  );

  /**
   * @deprecated Use {@link KafkaConsumerFactory#builder(String, Class) instead}
   */
  @Deprecated
  <T> KafkaConsumer<T> subscribe(
      String clientId,
      String topicName,
      String operationName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumeStrategy,
      Logger logger
  );

  <T> ConsumerBuilder<T> builder(String topicName, Class<T> messageClass);

}
