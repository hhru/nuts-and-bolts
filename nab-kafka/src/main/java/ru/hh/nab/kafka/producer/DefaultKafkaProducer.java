package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
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
        .thenApply(this::convertSpringSendResult);
  }

  private <T> KafkaSendResult<T> convertSpringSendResult(SendResult<String, Object> springResult) {
    return new KafkaSendResult<>(
        convertProducerRecord(springResult.getProducerRecord()),
        springResult.getRecordMetadata()
    );
  }

  private <T> ProducerRecord<String, T> convertProducerRecord(ProducerRecord<String, Object> initial) {
    return new ProducerRecord<>(
        initial.topic(),
        initial.partition(),
        initial.timestamp(),
        initial.key(),
        (T) initial.value(),
        initial.headers()
    );
  }
}
