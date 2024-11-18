package ru.hh.nab.kafka.consumer;


public interface KafkaConsumerFactory {

  <T> ConsumerBuilder<T> builder(String topicName, Class<T> messageClass);

}
