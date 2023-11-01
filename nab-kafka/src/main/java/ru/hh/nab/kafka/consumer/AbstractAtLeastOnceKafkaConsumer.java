package ru.hh.nab.kafka.consumer;

import java.io.Closeable;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for simple kafka consumer. Subscribes to topic in constructor.
 * Implements {@link Closeable} to enable auto-stopping consumer on Spring context shutdown
 */
@SuppressWarnings("unused")
public abstract class AbstractAtLeastOnceKafkaConsumer<M> implements ConsumeStrategy<M>, Closeable {
  protected final Logger logger;
  protected final KafkaConsumer<M> kafkaConsumer;

  protected AbstractAtLeastOnceKafkaConsumer(KafkaConsumerFactory kafkaConsumerFactory, String topic, String operationName, Class<M> messageClass) {
    this(kafkaConsumerFactory, topic, operationName, messageClass, null);
  }

  protected AbstractAtLeastOnceKafkaConsumer(
      KafkaConsumerFactory consumerFactory,
      String topic,
      String operationName,
      Class<M> messageClass,
      Logger logger) {
    if (topic == null || topic.isBlank()) {
      throw new IllegalArgumentException("topic required");
    }
    if (operationName == null || operationName.isBlank()) {
      throw new IllegalArgumentException("operationName required");
    }
    if (logger == null) {
      logger = LoggerFactory.getLogger(getClass());
    }

    this.logger = logger;
    this.kafkaConsumer = consumerFactory.subscribe(topic, operationName, messageClass, this, this.logger);
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, M>> messages, Ack<M> ack) throws InterruptedException {
    for (ConsumerRecord<String, M> record : messages) {
      processMessage(record.value());
      ack.seek(record);
    }
    ack.acknowledge();
  }

  @Override
  public void close() {
    kafkaConsumer.stop();
    logger.info("Stopped consumer");
  }

  protected abstract void processMessage(M message) throws InterruptedException;
}
