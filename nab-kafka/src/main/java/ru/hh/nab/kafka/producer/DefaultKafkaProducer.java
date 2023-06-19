package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    return kafkaTemplate.send((ProducerRecord<String, Object>) record)
        .thenApplyAsync(springResult -> convertSpringSendResult(springResult, (Class<T>) record.value().getClass()), executor);
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
