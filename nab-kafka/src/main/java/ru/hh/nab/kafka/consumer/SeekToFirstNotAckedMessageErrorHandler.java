package ru.hh.nab.kafka.consumer;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.SynchronizationStrategy;
import java.time.Duration;
import java.util.Optional;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.slf4j.Logger;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import ru.hh.nab.metrics.Counters;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

class SeekToFirstNotAckedMessageErrorHandler<T> implements CommonErrorHandler {
  private static final String MALFORMED_MESSAGE_METRIC_NAME = "nab.kafka.errors.records.deserialization.count";
  private static final long SMALL_INTERVAL_THRESHOLD = 500;
  private static final int DEFAULT_SLEEP_INTERVAL = 100;
  private static final int SMALL_SLEEP_INTERVAL = 10;

  private final ThreadLocal<BackOffExecution> backOffs = new ThreadLocal<>();
  private final ThreadLocal<Long> lastInterval = new ThreadLocal<>();

  private final Logger logger;
  private final BackOff backOff;
  private final KafkaConsumer<T> kafkaConsumer;

  private StatsDSender statsDSender;
  private Counters malformedMessagesCounter;
  private Tag serviceNameTag;

  private final Bucket errorsTokenBucket = Bucket
      .builder()
      // We do not care about contention because we are running in single thread, but we do care about memory allocations
      .withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED)
      // Interval precision for a greedy refill
      .withMillisecondPrecision()
      // Once per second
      .addLimit(limit -> limit
          .capacity(1)
          .refillGreedy(1, Duration.ofSeconds(1)))
      .build();

  public SeekToFirstNotAckedMessageErrorHandler(
      Logger logger,
      BackOff backOff,
      KafkaConsumer<T> kafkaConsumer,
      String serviceName,
      StatsDSender statsDSender
  ) {
    this.logger = logger;
    this.backOff = backOff;
    this.kafkaConsumer = kafkaConsumer;

    if (statsDSender != null) {
      this.malformedMessagesCounter = new Counters(2000);
      this.serviceNameTag = new Tag(Tag.APP_TAG_NAME, serviceName);
      this.statsDSender = statsDSender;
      statsDSender.sendPeriodically(() -> {
        statsDSender.sendCounters(MALFORMED_MESSAGE_METRIC_NAME, malformedMessagesCounter);
      });
    }
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
        long timeout = System.currentTimeMillis() + interval;
        long sleepInterval = interval > SMALL_INTERVAL_THRESHOLD ? DEFAULT_SLEEP_INTERVAL : Math.min(interval, SMALL_SLEEP_INTERVAL);
        while (System.currentTimeMillis() < timeout && container.isRunning()) {
          try {
            //noinspection BusyWait
            Thread.sleep(sleepInterval);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }
  }

  @Override
  public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
    if (thrownException instanceof RecordDeserializationException se) {

      if (statsDSender != null) {
        malformedMessagesCounter.add(1, this.serviceNameTag);
      }

      // In some scenarios, we can overwhelm our logging system.
      // In specific, we had an issues with sorm-backend consuming over 10% of workload with corrupted messages.
      if (errorsTokenBucket.tryConsume(1)) {
        logger.info("Skipping message of partition {} with offset {}. Log appears only once per second.", se.topicPartition(), se.offset());
      }

      try {
        consumer.seek(se.topicPartition(), se.offset() + 1);
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
