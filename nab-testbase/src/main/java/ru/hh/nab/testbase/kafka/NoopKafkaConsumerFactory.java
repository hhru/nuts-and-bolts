package ru.hh.nab.testbase.kafka;

import ru.hh.nab.kafka.consumer.ConsumerBuilder;
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;

@SuppressWarnings("unused")
public class NoopKafkaConsumerFactory implements KafkaConsumerFactory {

  @Override
  public <T> ConsumerBuilder<T> builder(String topicName, Class<T> messageClass) {
    return new NoopConsumerBuilder<>();
  }
}
