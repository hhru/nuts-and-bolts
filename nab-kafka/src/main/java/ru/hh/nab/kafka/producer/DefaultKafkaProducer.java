package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

public class DefaultKafkaProducer<T> implements KafkaProducer<T> {

  private final KafkaTemplate<String, T> kafkaTemplate;

  public DefaultKafkaProducer(KafkaTemplate<String, T> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public CompletableFuture<SendResult<String, T>> sendMessage(String topicName, T kafkaMessage) {
    return sendMessage(topicName, null, kafkaMessage);
  }

  public CompletableFuture<SendResult<String, T>> sendMessage(String topicName, String key, T kafkaMessage) {
    return kafkaTemplate.send(topicName, key, kafkaMessage).completable();
  }
}
