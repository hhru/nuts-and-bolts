package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;

public interface KafkaProducer {

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, Class<T> messageType, T kafkaMessage);

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key,  Class<T> messageType, T kafkaMessage);
}
