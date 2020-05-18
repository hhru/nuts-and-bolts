package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface KafkaProducer {

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage);

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage, Executor executor);

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage);

  <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage, Executor executor);

}
