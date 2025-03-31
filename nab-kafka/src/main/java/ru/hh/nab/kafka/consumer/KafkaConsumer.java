package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.util.CollectionUtils;

public class KafkaConsumer<T> implements SmartLifecycle {
  final KafkaConsumer<T> retryKafkaConsumer;
  private final Logger logger;
  private final Lock restartLock = new ReentrantLock();
  private final ConsumerMetadata consumerMetadata;
  private final Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider;
  private final BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerForPartitionsProvider;
  private final AckProvider<T> ackProvider;
  private final ConsumeStrategy<T> consumeStrategy;
  private final RetryQueue<T> retryQueue;
  private final DeadLetterQueue<T> deadLetterQueue;
  private final ConsumerContext<T> consumerContext;
  private final TopicPartitionsMonitoring topicPartitionsMonitoring;
  private final Duration checkNewPartitionsInterval;
  private volatile boolean running = false;
  private List<PartitionInfo> assignedPartitions;
  private volatile ScheduledFuture<?> checkPartitionsChangeFuture;
  private volatile AbstractMessageListenerContainer<String, T> currentSpringKafkaContainer;

  public KafkaConsumer(
      ConsumerMetadata consumerMetadata,
      ConsumeStrategy<T> consumeStrategy,
      RetryQueue<T> retryQueue,
      KafkaConsumer<T> retryKafkaConsumer,
      DeadLetterQueue<T> deadLetterQueue,
      Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider,
      AckProvider<T> ackProvider,
      Logger logger
  ) {
    this.consumerMetadata = consumerMetadata;
    this.consumeStrategy = consumeStrategy;
    this.retryQueue = retryQueue;
    this.deadLetterQueue = deadLetterQueue;
    this.retryKafkaConsumer = retryKafkaConsumer;
    this.ackProvider = ackProvider;
    this.logger = logger;
    this.consumerContext = new ConsumerContext<>();

    this.springContainerProvider = springContainerProvider;
    this.springContainerForPartitionsProvider = null;
    this.topicPartitionsMonitoring = null;
    this.checkNewPartitionsInterval = null;
    this.assignedPartitions = null;
    createNewSpringContainer();
  }

  public KafkaConsumer(
      ConsumerMetadata consumerMetadata,
      ConsumeStrategy<T> consumeStrategy,
      DeadLetterQueue<T> deadLetterQueue,
      BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerForPartitionsProvider,
      TopicPartitionsMonitoring topicPartitionsMonitoring,
      ClusterMetadataProvider clusterMetadataProvider,
      AckProvider<T> ackProvider,
      Logger logger,
      Duration checkNewPartitionsInterval
  ) {
    this.consumerMetadata = consumerMetadata;
    this.consumeStrategy = consumeStrategy;
    this.deadLetterQueue = deadLetterQueue;
    this.logger = logger;
    this.retryQueue = null;
    this.retryKafkaConsumer = null;
    this.ackProvider = ackProvider;
    this.consumerContext = new ConsumerContext<>();

    this.springContainerProvider = null;
    this.springContainerForPartitionsProvider = springContainerForPartitionsProvider;
    this.topicPartitionsMonitoring = topicPartitionsMonitoring;
    this.checkNewPartitionsInterval = checkNewPartitionsInterval;
    this.assignedPartitions = clusterMetadataProvider.getPartitionsInfo(consumerMetadata.getTopic());
    createNewSpringContainer();
  }

  public boolean isRunning() {
    return running;
  }

  public void start() {
    restartLock.lock();
    try {
      if (running) {
        return;
      }
      running = true;
      currentSpringKafkaContainer.start();
      if (checkNewPartitionsInterval != null && this.assignedPartitions != null) {
        subscribeForAssignedPartitionsChange();
      }
      if (retryKafkaConsumer != null) {
        retryKafkaConsumer.start();
      }
      if (readingAllPartitions()) {
        logger.info("Subscribed for {}, reading all partitions, operation={}", consumerMetadata.getTopic(), consumerMetadata.getOperation());
      } else {
        logger.info("Subscribed for {}, consumer group id {}", consumerMetadata.getTopic(), consumerMetadata.getConsumerGroupId());
      }
    } finally {
      restartLock.unlock();
    }
  }

  private boolean readingAllPartitions() {
    return springContainerForPartitionsProvider != null;
  }

  private void subscribeForAssignedPartitionsChange() {
    this.checkPartitionsChangeFuture = topicPartitionsMonitoring.subscribeOnPartitionsChange(
        consumerMetadata.getTopic(),
        checkNewPartitionsInterval,
        assignedPartitions,
        newPartitions -> {
          restartLock.lock();
          try {
            if (!running) {
              stopPartitionsMonitoring();
              return;
            }
            if (!currentSpringKafkaContainer.isRunning()) {
              return;
            }
            currentSpringKafkaContainer.stop();
            this.assignedPartitions = newPartitions;
            createNewSpringContainer();
            currentSpringKafkaContainer.start();
          } finally {
            restartLock.unlock();
          }
        }
    );
  }

  public void stop() {
    restartLock.lock();
    try {
      if (!running) {
        return;
      }
      running = false;
      currentSpringKafkaContainer.stop();
      stopPartitionsMonitoring();
      if (retryKafkaConsumer != null) {
        retryKafkaConsumer.stop();
      }
      if (readingAllPartitions()) {
        logger.info("Stopped kafka consumer for all partitions of {}, operation={}", consumerMetadata.getTopic(), consumerMetadata.getOperation());
      } else {
        logger.info("Stopped kafka consumer for {} with consumer group id {}", consumerMetadata.getTopic(), consumerMetadata.getConsumerGroupId());
      }
    } finally {
      restartLock.unlock();
    }
  }

  private void stopPartitionsMonitoring() {
    if (checkPartitionsChangeFuture != null) {
      checkPartitionsChangeFuture.cancel(false);
    }
  }

  public Collection<TopicPartition> getAssignedPartitions() {
    if (assignedPartitions != null) {
      return assignedPartitions.stream().map(p -> new TopicPartition(p.topic(), p.partition())).toList();
    }
    return currentSpringKafkaContainer.getAssignedPartitions();
  }

  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer) {
    consumerContext.prepareForNextBatch(messages);
    Ack<T> ack = ackProvider.createAck(this, consumer);
    processMessages(messages, ack);
    rewindToLastAckedOffset(consumer);
  }

  private void processMessages(List<ConsumerRecord<String, T>> messages, Ack<T> ack) {
    try {
      consumeStrategy.onMessagesBatch(messages, ack);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted thread during kafka processing", e);
    }
  }

  protected void createNewSpringContainer() {
    if (springContainerProvider != null) {
      currentSpringKafkaContainer = springContainerProvider.apply(this);
      return;
    }
    currentSpringKafkaContainer = springContainerForPartitionsProvider.apply(this, assignedPartitions);
  }

  public void rewindToLastAckedOffset(Consumer<?, ?> consumer) {
    if (consumerContext.isWholeBatchAcked()) {
      return;
    }

    List<ConsumerRecord<String, T>> messages = consumerContext.getCurrentBatch();
    if (CollectionUtils.isEmpty(messages)) {
      return;
    }

    LinkedHashMap<TopicPartition, OffsetAndMetadata> offsetsToSeek = getLowestOffsetsForEachPartition(messages);
    offsetsToSeek.putAll(consumerContext.getBatchSeekedOffsets());
    offsetsToSeek.forEach(consumer::seek);
  }


  private LinkedHashMap<TopicPartition, OffsetAndMetadata> getLowestOffsetsForEachPartition(List<ConsumerRecord<String, T>> messages) {
    return messages.stream().collect(toMap(
        record -> new TopicPartition(record.topic(), record.partition()),
        record -> new OffsetAndMetadata(record.offset()),
        BinaryOperator.minBy(Comparator.comparing(OffsetAndMetadata::offset)),
        LinkedHashMap::new
    ));
  }

  ConsumerContext<T> getConsumerContext() {
    return consumerContext;
  }

  DeadLetterQueue<T> getDeadLetterQueue() {
    return deadLetterQueue;
  }

  RetryQueue<T> getRetryQueue() {
    return retryQueue;
  }
}
