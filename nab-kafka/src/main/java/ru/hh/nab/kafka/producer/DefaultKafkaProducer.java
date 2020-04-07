package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

public class DefaultKafkaProducer implements KafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final Executor executor;

  public DefaultKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topicName, T kafkaMessage) {
    return sendMessage(topicName, null, kafkaMessage);
  }

  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topicName, String key, T kafkaMessage) {
    return CompletableFuture.supplyAsync(() -> kafkaTemplate.send(topicName, key, kafkaMessage), executor)
        .thenCompose(ListenableFuture::completable)
        .thenApply(springResult -> convertSpringSendResult(springResult, (Class<T>) kafkaMessage.getClass()));
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
