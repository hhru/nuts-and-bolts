package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.BatchConsumerAwareMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.support.TopicPartitionOffset;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.AUTH_EXCEPTION_RETRY_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.CONCURRENCY;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_POLL_TIMEOUT_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.POLL_TIMEOUT;

public class DefaultConsumerBuilder<T> implements ConsumerBuilder<T> {

  private final String topicName;
  private final Class<T> messageClass;
  private final DefaultConsumerFactory consumerFactory;
  private String operationName;
  private String clientId;
  private boolean useConsumerGroup;
  private SeekPosition seekPositionIfNoConsumerGroup;
  private Duration checkNewPartitionsInterval;

  private ConsumeStrategy<T> consumeStrategy;
  private RetryPolicyResolver<T> retryPolicyResolver;
  private KafkaProducer retryProducer;
  private RetryTopics retryTopics;
  private ConsumeStrategy<T> retryConsumeStrategy;
  private Logger logger;
  private AckProvider<T> ackProvider;

  public DefaultConsumerBuilder(DefaultConsumerFactory consumerFactory, String topicName, Class<T> messageClass) {
    this.topicName = topicName;
    this.messageClass = messageClass;
    this.consumerFactory = consumerFactory;
    withConsumerGroup();
  }

  @Override
  public DefaultConsumerBuilder<T> withOperationName(String operationName) {
    this.operationName = operationName;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy) {
    this.consumeStrategy = consumeStrategy;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver, RetryTopics retryTopics) {
    this.retryProducer = retryProducer;
    this.retryPolicyResolver = retryPolicyResolver;
    this.retryTopics = retryTopics;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withRetryConsumeStrategy(ConsumeStrategy<T> retryConsumeStrategy) {
    this.retryConsumeStrategy = retryConsumeStrategy;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withConsumerGroup() {
    this.useConsumerGroup = true;
    this.ackProvider = KafkaInternalTopicAck::new;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition, Duration checkNewPartitionsInterval) {
    this.useConsumerGroup = false;
    this.seekPositionIfNoConsumerGroup = seekPosition;
    this.ackProvider = (kafkaConsumer, nativeKafkaConsumer, retryService) -> new InMemorySeekOnlyAck<>(kafkaConsumer);
    this.checkNewPartitionsInterval = checkNewPartitionsInterval;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition) {
    return withAllPartitionsAssigned(seekPosition, Duration.ofMinutes(5));
  }

  @Override
  public DefaultConsumerBuilder<T> withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Override
  public KafkaConsumer<T> build() {
    ConfigProvider configProvider = consumerFactory.getConfigProvider();
    ConsumerFactory<String, T> springConsumerFactory = consumerFactory.getSpringConsumerFactory(topicName, messageClass);
    ConsumerMetadata consumerMetadata = new ConsumerMetadata(configProvider.getServiceName(), topicName, operationName);
    if (useConsumerGroup) {
      RetryService<T> retryService = null;
      KafkaConsumer<T> retryKafkaConsumer = null;
      if (usingRetries()) {
        if (RetryTopics.DEFAULT_SINGLE_TOPIC == retryTopics) {
          retryTopics = RetryTopics.defaultSingleTopic(consumerMetadata);
        } else if (RetryTopics.DEFAULT_PAIR_OF_TOPICS == retryTopics) {
          retryTopics = RetryTopics.defaultPairOfTopics(consumerMetadata);
        }
        retryService = new RetryService<>(retryProducer, retryTopics.retrySendTopic(), retryPolicyResolver);
        retryKafkaConsumer = buildRetryKafkaConsumer(retryService);
      }
      return buildKafkaConsumerForConsumerGroup(
          configProvider,
          springConsumerFactory,
          consumerMetadata,
          retryService,
          retryKafkaConsumer
      );
    }
    return buildKafkaConsumerForAllPartitions(configProvider, springConsumerFactory, consumerMetadata);
  }

  private boolean usingRetries() {
    return retryPolicyResolver != null;
  }

  private KafkaConsumer<T> buildKafkaConsumerForConsumerGroup(
      ConfigProvider configProvider,
      ConsumerFactory<String, T> springConsumerFactory,
      ConsumerMetadata consumerMetadata,
      RetryService<T> retryService,
      KafkaConsumer<T> retryKafkaConsumer
  ) {
    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (nabKafkaConsumer) -> {
      ContainerProperties containerProperties = getSpringConsumerContainerPropertiesWithConsumerGroup(
          configProvider,
          consumerMetadata,
          (BatchConsumerAwareMessageListener<String, T>) nabKafkaConsumer::onMessagesBatch
      );
      return getSpringKafkaListenerContainer(configProvider, springConsumerFactory, nabKafkaConsumer, containerProperties);
    };

    return new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, consumeStrategy),
        retryService,
        retryKafkaConsumer,
        springContainerProvider,
        ackProvider,
        logger
    );
  }

  private KafkaConsumer<T> buildRetryKafkaConsumer(RetryService<T> retryService) {
    ConfigProvider configProvider = consumerFactory.getConfigProvider();
    String retryReceiveTopicName = retryTopics.retryReceiveTopic();
    ConsumerFactory<String, T> springConsumerFactory = consumerFactory.getSpringConsumerFactory(retryReceiveTopicName, messageClass);
    ConsumerMetadata consumerMetadata = new ConsumerMetadata(configProvider.getServiceName(), retryReceiveTopicName, "");
    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (nabKafkaConsumer) -> {
      ContainerProperties containerProperties = getSpringConsumerContainerPropertiesWithConsumerGroup(
          configProvider,
          consumerMetadata,
          (BatchConsumerAwareMessageListener<String, T>) nabKafkaConsumer::onMessagesBatch
      );
      return getSpringKafkaListenerContainer(configProvider, springConsumerFactory, nabKafkaConsumer, containerProperties);
    };
    ConsumeStrategy<T> retryReceiveConsumeStrategy = getRetryReceiveConsumeStrategy();
    return new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, retryReceiveConsumeStrategy),
        retryService,
        null, // retry consumer has no retry consumer
        springContainerProvider,
        ackProvider,
        logger
    );
  }

  private ConsumeStrategy<T> getRetryReceiveConsumeStrategy() {
    if (retryConsumeStrategy != null) {
      return retryConsumeStrategy;
    }
    if (!retryTopics.isSingleTopic()) {
      return consumeStrategy;
    }
    long pollTimeout = consumerFactory.configProvider.getNabConsumerSettings(topicName).getLong(POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS);
    return decorateForDelayedRetry(consumeStrategy, Duration.ofMillis(pollTimeout * 9 / 10));
  }

  /**
   * Decorate consume strategy to process messages only when they are ready for retry.
   *
   * @see  HeadersMessageMetadataProvider#getNextRetryTime
   * @see SimpleDelayedConsumeStrategy
   * */
  public static <T> SimpleDelayedConsumeStrategy<T> decorateForDelayedRetry(ConsumeStrategy<T> delegate, Duration sleepIfNotReadyDuration) {
    return new SimpleDelayedConsumeStrategy<>(
        delegate,
        message -> HeadersMessageMetadataProvider.getNextRetryTime(message.headers()).orElse(Instant.EPOCH),
        sleepIfNotReadyDuration
    );
  }

  private KafkaConsumer<T> buildKafkaConsumerForAllPartitions(
      ConfigProvider configProvider, ConsumerFactory<String, T> springConsumerFactory, ConsumerMetadata consumerMetadata
  ) {
    if (usingRetries()) {
      throw new IllegalStateException("Can't use retries for consumer reading all partitions");
    }

    BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (
        nabKafkaConsumer,
        partitionsInfo
    ) -> {
      ContainerProperties containerProperties = getSpringConsumerContainerPropertiesSubscribedToAllPartitions(
          configProvider,
          (BatchConsumerAwareMessageListener<String, T>) nabKafkaConsumer::onMessagesBatch,
          partitionsInfo,
          nabKafkaConsumer
      );
      return getSpringKafkaListenerContainer(configProvider, springConsumerFactory, nabKafkaConsumer, containerProperties);
    };

    return new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, consumeStrategy),
        springContainerProvider,
        consumerFactory.getTopicPartitionsMonitoring(),
        consumerFactory.getClusterMetadataProvider(),
        ackProvider,
        logger,
        checkNewPartitionsInterval
    );
  }


  private ConcurrentMessageListenerContainer<String, T> getSpringKafkaListenerContainer(
      ConfigProvider configProvider, ConsumerFactory<String, T> springConsumerFactory, KafkaConsumer<T> nabKafkaConsumer,
      ContainerProperties containerProperties
  ) {
    SeekToFirstNotAckedMessageErrorHandler<T> errorHandler = consumerFactory.getCommonErrorHandler(topicName, nabKafkaConsumer, logger);
    ConcurrentMessageListenerContainer<String, T> container = new ConcurrentMessageListenerContainer<>(springConsumerFactory, containerProperties);
    container.setCommonErrorHandler(errorHandler);
    container.setConcurrency(configProvider.getNabConsumerSettings(topicName).getInteger(CONCURRENCY, 1));
    return container;
  }

  private ContainerProperties getSpringConsumerContainerPropertiesSubscribedToAllPartitions(
      ConfigProvider configProvider,
      GenericMessageListener<?> messageListener,
      List<PartitionInfo> partitionsInfo,
      KafkaConsumer<T> nabKafkaConsumer
  ) {
    ConsumerConsumingState<T> consumingState = nabKafkaConsumer.getConsumingState();
    TopicPartitionOffset[] partitions = partitionsInfo
        .stream()
        .map(partition -> consumingState
            .getGlobalSeekedOffset(new TopicPartition(topicName, partition.partition()))
            .map(seekedOffset -> new TopicPartitionOffset(topicName, partition.partition(), seekedOffset.offset()))
            .orElseGet(() -> new TopicPartitionOffset(topicName, partition.partition(), seekPositionIfNoConsumerGroup.getSpringSeekPosition()))
        )
        .toArray(TopicPartitionOffset[]::new);

    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(topicName);
    var containerProperties = new ContainerProperties(partitions);
    addCommonContainerProperties(messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private ContainerProperties getSpringConsumerContainerPropertiesWithConsumerGroup(
      ConfigProvider configProvider, ConsumerMetadata consumerMetadata, GenericMessageListener<?> messageListener
  ) {
    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(consumerMetadata.getTopic());
    var containerProperties = new ContainerProperties(consumerMetadata.getTopic());
    containerProperties.setGroupId(consumerMetadata.getConsumerGroupId());
    addCommonContainerProperties(messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private void addCommonContainerProperties(
      GenericMessageListener<?> messageListener, ContainerProperties containerProperties, FileSettings nabConsumerSettings
  ) {
    containerProperties.setClientId(Optional.ofNullable(clientId).orElseGet(() -> UUID.randomUUID().toString()));
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    containerProperties.setPollTimeout(nabConsumerSettings.getLong(POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS));
    containerProperties.setAuthExceptionRetryInterval(Duration.ofMillis(nabConsumerSettings.getLong(
        AUTH_EXCEPTION_RETRY_INTERVAL,
        DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS
    )));
  }

}
