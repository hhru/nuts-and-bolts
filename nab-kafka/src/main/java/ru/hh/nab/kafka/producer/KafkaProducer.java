package ru.hh.nab.kafka.producer;

import org.springframework.kafka.support.SendResult;
import java.util.concurrent.CompletableFuture;

public interface KafkaProducer<T> {

  CompletableFuture<SendResult<String, T>> sendMessage(String topic, T kafkaMessage);

  CompletableFuture<SendResult<String, T>> sendMessage(String topic, String key, T kafkaMessage);
}
