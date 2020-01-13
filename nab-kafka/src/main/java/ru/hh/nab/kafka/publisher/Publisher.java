package ru.hh.nab.kafka.publisher;

import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

public class Publisher<T> {

  private final String topic;
  private final KafkaTemplate<String, T> kafkaTemplate;

  public Publisher(String topic, KafkaTemplate<String, T> kafkaTemplate) {
    this.topic = topic;
    this.kafkaTemplate = kafkaTemplate;
  }

  public CompletableFuture<SendResult<String, T>> sendMessage(T kafkaMessage) {
    return sendMessage(null, kafkaMessage);
  }

  public CompletableFuture<SendResult<String, T>> sendMessage(String key, T kafkaMessage) {
    return kafkaTemplate.send(topic, key, kafkaMessage).completable();
  }
}
