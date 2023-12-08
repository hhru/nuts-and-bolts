package ru.hh.nab.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
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


  <T> KafkaConsumer<T> subscribe(String topicName,
                                 String operationName,
                                 TypeReference<T> typeReference,
                                 ConsumeStrategy<T> messageConsumer);


  <T> KafkaConsumer<T> subscribe(String topicName,
                                 String operationName,
                                 TypeReference<T> typeReference,
                                 ConsumeStrategy<T> messageConsumer,
                                 Logger logger);
}
