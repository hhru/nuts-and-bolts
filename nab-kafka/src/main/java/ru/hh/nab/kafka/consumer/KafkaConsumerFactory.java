package ru.hh.nab.kafka.consumer;

import org.slf4j.Logger;

public interface KafkaConsumerFactory {

  <T> KafkaConsumer<T> subscribe(String topicName,
                                 String operationName,
                                 Class<T> messageClass,
                                 ConsumeStrategy<T> messageConsumer);

  <T> KafkaConsumer<T> subscribe(String topicName,
                                 String operationName,
                                 Class<T> messageClass,
                                 ConsumeStrategy<T> messageConsumer,
                                 Logger logger);

  <T> KafkaConsumer<T> subscribe(String clientId,
                                 String topicName,
                                 String operationName,
                                 Class<T> messageClass,
                                 ConsumeStrategy<T> messageConsumer,
                                 Logger logger);

}
