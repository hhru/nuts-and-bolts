package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.util.CollectionUtils;

public class KafkaConsumer<T> {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

  private final ConsumerDescription consumerDescription;
  private final Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider;
  private final BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerForPartitionsProvider;

  private final BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider;
  private final ConsumeStrategy<T> consumeStrategy;
  private final ConsumerConsumingState<T> consumerConsumingState;
  private final TopicPartitionsMonitoring topicPartitionsMonitoring;
  private final Duration checkNewPartitionsInterval;


  private AbstractMessageListenerContainer<String, T> currentSpringKafkaContainer;
  private List<PartitionInfo> assignedPartitions;

  public KafkaConsumer(
      ConsumerDescription consumerDescription,
      ConsumeStrategy<T> consumeStrategy,
      Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider,
      BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider
  ) {
    this.consumerDescription = consumerDescription;
    this.consumeStrategy = consumeStrategy;
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
      ConsumerDescription consumerDescription,
      ConsumeStrategy<T> consumeStrategy,
      BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerForPartitionsProvider,
      TopicPartitionsMonitoring topicPartitionsMonitoring,
      ClusterMetaInfoProvider clusterMetaInfoProvider,
      BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider,
      Duration checkNewPartitionsInterval
  ) {
    this.consumerDescription = consumerDescription;
    this.consumeStrategy = consumeStrategy;
    this.ackProvider = ackProvider;

    this.consumerConsumingState = new ConsumerConsumingState<>();

    this.springContainerProvider = null;
    this.springContainerForPartitionsProvider = springContainerForPartitionsProvider;
    this.topicPartitionsMonitoring = topicPartitionsMonitoring;
    this.checkNewPartitionsInterval = checkNewPartitionsInterval;
    this.assignedPartitions = clusterMetaInfoProvider.getPartitionsInfo(consumerDescription.getTopic());
    createNewSpringContainer();
  }

  public void start() {
    running.set(true);
    currentSpringKafkaContainer.start();
    if (checkNewPartitionsInterval != null && this.assignedPartitions != null) {
      topicPartitionsMonitoring.trackPartitionsChanges(
          this,
          consumerDescription.getTopic(),
          checkNewPartitionsInterval,
          this.assignedPartitions,
          (prevPartitions, actualPartitions) -> {
            synchronized (running) {
              if (!running.get() || !currentSpringKafkaContainer.isRunning()) {
                return;
              }
              currentSpringKafkaContainer.stop(() -> {
                LOGGER.info(
                    "reconnection for topic {} due to partitions change, prev={}, new={}",
                    consumerDescription.getTopic(),
                    prevPartitions,
                    actualPartitions
                );
                this.assignedPartitions = actualPartitions;
                createNewSpringContainer();
                currentSpringKafkaContainer.start();
              });
            }
          }
      );
      topicPartitionsMonitoring.startScheduling();
    }
  }

  public void stop(Runnable callback) {
    synchronized (running) {
      running.set(false);
      currentSpringKafkaContainer.stop(callback);
      removeAssignedCallbacks();
    }
  }

  public void stop() {
    synchronized (running) {
      running.set(false);
      currentSpringKafkaContainer.stop();
      removeAssignedCallbacks();
    }
  }

  private void removeAssignedCallbacks() {
    if (topicPartitionsMonitoring != null) {
      topicPartitionsMonitoring.clearCallback(this);
    }
  }

  public Collection<TopicPartition> getAssignedPartitions() {
    return currentSpringKafkaContainer.getAssignedPartitions();
  }

  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer) {
    consumerConsumingState.prepareForNextBatch(messages);
    Ack<T> ack = ackProvider.apply(this, consumer);
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

  private void createNewSpringContainer() {
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
