package ru.hh.nab.kafka.consumer;

import java.util.Optional;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.slf4j.Logger;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

class SeekToFirstNotAckedMessageErrorHandler<T> implements CommonErrorHandler {

  private final ThreadLocal<BackOffExecution> backOffs = new ThreadLocal<>();

  private final ThreadLocal<Long> lastInterval = new ThreadLocal<>();

  private final Logger logger;
  private final BackOff backOff;
  private final KafkaConsumer<T> kafkaConsumer;

  public SeekToFirstNotAckedMessageErrorHandler(
      Logger logger,
      BackOff backOff,
      KafkaConsumer<T> kafkaConsumer
  ) {
    this.logger = logger;
    this.backOff = backOff;
    this.kafkaConsumer = kafkaConsumer;
  }

  @Override
  public void handleBatch(
      Exception thrownException,
      ConsumerRecords<?, ?> data,
      Consumer<?, ?> consumer,
      MessageListenerContainer container,
      Runnable invokeListener
  ) {
    // Since spring-kafka wraps our "business" exception by org.springframework.kafka.listener.ListenerExecutionFailedException
    var exceptionToLog = Optional.of(thrownException).map(Throwable::getCause).orElse(thrownException);
    logger.error("exception during kafka processing", exceptionToLog);

    kafkaConsumer.rewindToLastAckedOffset(consumer);

    if (this.backOff != null) {
      BackOffExecution backOffExecution = this.backOffs.get();
      if (backOffExecution == null) {
        backOffExecution = this.backOff.start();
        this.backOffs.set(backOffExecution);
      }
      Long interval = backOffExecution.nextBackOff();
      if (interval == BackOffExecution.STOP) {
        interval = this.lastInterval.get();
        if (interval == null) {
          interval = 0L;
        }
      }
      this.lastInterval.set(interval);
      if (interval > 0) {
        try {
          Thread.sleep(interval);
        } catch (@SuppressWarnings("unused") InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Override
  public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
    if (thrownException instanceof RecordDeserializationException se) {
      try {
        logger.debug("Skipping message of partition {} with offset {}", se.topicPartition(), se.offset());
        consumer.seek(se.topicPartition(), se.offset() + 1);
        consumer.commitSync();
      } catch (Exception e) {
        logger.error("Failed to skip malformed message", e);
      }
    } else {
      CommonErrorHandler.super.handleOtherException(thrownException, consumer, container, batchListener);
    }
  }

  @Override
  public void clearThreadState() {
    this.backOffs.remove();
    this.lastInterval.remove();
  }

  @Override
  public boolean isAckAfterHandle() {
    // disable, because our handle method just log the exception without throwing it.
    // all ACK logic should be performed in nab-kafka.messageHandler
    return false;
  }
}
