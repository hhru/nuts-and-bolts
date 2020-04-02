package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;

public interface KafkaProducer {

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage);

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage);
}
