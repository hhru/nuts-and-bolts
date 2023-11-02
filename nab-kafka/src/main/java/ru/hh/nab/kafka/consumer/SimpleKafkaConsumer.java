package ru.hh.nab.kafka.consumer;

import java.io.Closeable;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for simple kafka consumer. Subscribes to topic in constructor.
 * Implements {@link Closeable} to enable auto-stopping consumer on Spring context shutdown
 */
@SuppressWarnings("unused")
public abstract class SimpleKafkaConsumer<M> implements MessageProcessor<M>, Closeable {
  protected final Logger logger;
  protected final KafkaConsumer<M> kafkaConsumer;

  protected SimpleKafkaConsumer(
      KafkaConsumerFactory kafkaConsumerFactory, 
      String topic, 
      String operationName,
      Class<M> messageClass) {
    this(kafkaConsumerFactory, topic, operationName, ConsumeStrategy::atLeastOnceWithBatchAck, messageClass);
  }

  protected SimpleKafkaConsumer(
      KafkaConsumerFactory kafkaConsumerFactory, 
      String topic, 
      String operationName,
      Function<MessageProcessor<M>, ConsumeStrategy<M>> consumeStrategyCreator,
      Class<M> messageClass) {
    this(kafkaConsumerFactory, topic, operationName, consumeStrategyCreator, messageClass, null);
  }

  protected SimpleKafkaConsumer(
      KafkaConsumerFactory consumerFactory,
      String topic,
      String operationName,
      Function<MessageProcessor<M>, ConsumeStrategy<M>> consumeStrategyCreator,
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
    this.kafkaConsumer = consumerFactory.subscribe(topic, operationName, messageClass, consumeStrategyCreator.apply(this), this.logger);
  }

  @Override
  public void close() {
    kafkaConsumer.stop();
    logger.info("Stopped consumer");
  }

  @Override
  public abstract void process(M message) throws InterruptedException;
}
