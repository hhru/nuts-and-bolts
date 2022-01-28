package ru.hh.nab.kafka.consumer;

import java.util.Optional;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.springframework.kafka.listener.ContainerAwareBatchErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

class SeekToFirstNotAckedMessageErrorHandler<T> implements ContainerAwareBatchErrorHandler {

  private final ThreadLocal<BackOffExecution> backOffs = new ThreadLocal<>();

  private final ThreadLocal<Long> lastInterval = new ThreadLocal<>();

  private final Logger logger;
  private final BackOff backOff;
  private final KafkaConsumer<T> kafkaConsumer;

  public SeekToFirstNotAckedMessageErrorHandler(Logger logger, BackOff backOff, KafkaConsumer<T> kafkaConsumer) {
    this.logger = logger;
    this.backOff = backOff;
    this.kafkaConsumer = kafkaConsumer;
  }

  @Override
  public void handle(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer,
                     MessageListenerContainer container) {
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
