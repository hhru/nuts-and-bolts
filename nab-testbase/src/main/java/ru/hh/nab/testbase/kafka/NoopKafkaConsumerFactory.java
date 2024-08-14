package ru.hh.nab.testbase.kafka;

import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerBuilder;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;

public class NoopKafkaConsumerFactory implements KafkaConsumerFactory {

  @Override
  public <T> KafkaConsumer<T> subscribe(String topicName, String operationName, Class<T> messageClass, ConsumeStrategy<T> consumeStrategy) {
    return new NoopKafkaConsumer<>();
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(
      String topicName,
      String operationName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumeStrategy,
      Logger logger
  ) {
    return new NoopKafkaConsumer<>();
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(
      String clientId,
      String topicName,
      String operationName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumeStrategy,
      Logger logger
  ) {
    return new NoopKafkaConsumer<>();
  }

  @Override
  public <T> ConsumerBuilder<T> builder(String topicName, Class<T> messageClass) {
    return new NoopConsumerBuilder<>();
  }
}
