package ru.hh.nab.kafka.producer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

public class DefaultKafkaProducer extends KafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  DefaultKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<KafkaSendResult<T>> sendMessage(ProducerRecord<String, T> record, Executor executor) {
    return CompletableFuture
        .supplyAsync(() -> kafkaTemplate.send((ProducerRecord<String, Object>) record), executor)
        .thenCompose(Function.identity())
        .thenApply(springResult -> convertSpringSendResult(springResult, (Class<T>) record.value().getClass()));
  }

  @Override
  public List<PartitionInfo> partitionsFor(String topic) {
    return kafkaTemplate.partitionsFor(topic);
  }

  private <T> KafkaSendResult<T> convertSpringSendResult(SendResult<String, Object> springResult, Class<T> messageType) {
    return new KafkaSendResult<>(
        convertProducerRecord(springResult.getProducerRecord(), messageType),
        springResult.getRecordMetadata()
    );
  }

  private <T> ProducerRecord<String, T> convertProducerRecord(ProducerRecord<String, Object> initial, Class<T> messageType) {
    return new ProducerRecord<>(
        initial.topic(),
        initial.partition(),
        initial.timestamp(),
        initial.key(),
        messageType.cast(initial.value()),
        initial.headers()
    );
  }
}
