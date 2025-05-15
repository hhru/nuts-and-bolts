package ru.hh.nab.kafka.producer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;

public abstract class KafkaProducer {

  public final <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage) {
    return sendMessage(topic, null, kafkaMessage, Runnable::run);
  }

  public final <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage, Executor executor) {
    return sendMessage(topic, null, kafkaMessage, executor);
  }

  public final <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage) {
    return sendMessage(topic, key, kafkaMessage, Runnable::run);
  }

  public final <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage, Executor executor) {
    return sendMessage(new ProducerRecord<>(topic, key, kafkaMessage), executor);
  }

  public abstract <T> CompletableFuture<KafkaSendResult<T>> sendMessage(ProducerRecord<String, T> record, Executor executor);

  public abstract List<PartitionInfo> partitionsFor(String topic);

}
