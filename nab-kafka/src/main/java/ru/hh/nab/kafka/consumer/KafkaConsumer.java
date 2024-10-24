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
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.util.CollectionUtils;

public class KafkaConsumer<T> implements SmartLifecycle {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);
  private volatile boolean running = false;
  private final Lock restartLock = new ReentrantLock();
  private final ConsumerMetadata consumerMetadata;
  private final Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider;
  private final BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerForPartitionsProvider;
  final KafkaConsumer<T> retryKafkaConsumer;
  private final AckProvider<T> ackProvider;
  private final ConsumeStrategy<T> consumeStrategy;
  private final RetryService<T> retryService;
  private final ConsumerConsumingState<T> consumerConsumingState;
  private final TopicPartitionsMonitoring topicPartitionsMonitoring;
  private final Duration checkNewPartitionsInterval;
  private List<PartitionInfo> assignedPartitions;
  private volatile ScheduledFuture<?> checkPartitionsChangeFuture;
  private volatile AbstractMessageListenerContainer<String, T> currentSpringKafkaContainer;

  public KafkaConsumer(
      ConsumerMetadata consumerMetadata,
      ConsumeStrategy<T> consumeStrategy,
      RetryService<T> retryService,
      KafkaConsumer<T> retryKafkaConsumer,
      Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider,
      AckProvider<T> ackProvider
  ) {
    this.consumerMetadata = consumerMetadata;
    this.consumeStrategy = consumeStrategy;
    this.retryService = retryService;
    this.retryKafkaConsumer = retryKafkaConsumer;
    this.ackProvider = ackProvider;
    this.consumerConsumingState = new ConsumerConsumingState<>();

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
      BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerForPartitionsProvider,
      TopicPartitionsMonitoring topicPartitionsMonitoring,
      ClusterMetadataProvider clusterMetadataProvider,
      AckProvider<T> ackProvider,
      Duration checkNewPartitionsInterval
  ) {
    this.consumerMetadata = consumerMetadata;
    this.consumeStrategy = consumeStrategy;
    this.retryService = null;
    this.retryKafkaConsumer = null;
    this.ackProvider = ackProvider;
    this.consumerConsumingState = new ConsumerConsumingState<>();

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
    } finally {
      restartLock.unlock();
    }
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
    consumerConsumingState.prepareForNextBatch(messages);
    Ack<T> ack = ackProvider.createAck(this, consumer, retryService);
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
    if (consumerConsumingState.isWholeBatchAcked()) {
      return;
    }

    List<ConsumerRecord<String, T>> messages = consumerConsumingState.getCurrentBatch();
    if (CollectionUtils.isEmpty(messages)) {
      return;
    }

    LinkedHashMap<TopicPartition, OffsetAndMetadata> offsetsToSeek = getLowestOffsetsForEachPartition(messages);
    offsetsToSeek.putAll(consumerConsumingState.getBatchSeekedOffsets());
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

  public ConsumerConsumingState<T> getConsumingState() {
    return consumerConsumingState;
  }

}
