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
  private boolean standaloneRetries;
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

  public ConsumerBuilder<T> withRetryProducer(KafkaProducer retryProducer) {
    this.retryProducer = retryProducer;
    return this;
  }

  @Override
  public ConsumerBuilder<T> withStandaloneRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver) {
    this.standaloneRetries = true;
    this.retryProducer = retryProducer;
    this.retryPolicyResolver = retryPolicyResolver;
    return this;
  }

  @Override
  public ConsumerBuilder<T> withExternalRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver) {
    this.standaloneRetries = false;
    this.retryProducer = retryProducer;
    this.retryPolicyResolver = retryPolicyResolver;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withAckProvider(AckProvider<T> ackProvider) {
    this.ackProvider = ackProvider;
    return this;
  }

  @Override
  public ConsumerBuilder<T> withConsumerGroup() {
    this.useConsumerGroup = true;
    withAckProvider(KafkaInternalTopicAck::new);
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition, Duration checkNewPartitionsInterval) {
    this.useConsumerGroup = false;
    this.seekPositionIfNoConsumerGroup = seekPosition;
    withAckProvider((kafkaConsumer, nativeKafkaConsumer, retryService) -> new InMemorySeekOnlyAck<>(kafkaConsumer));
    this.checkNewPartitionsInterval = checkNewPartitionsInterval;
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition) {
    return withAllPartitionsAssigned(seekPosition, Duration.ofMinutes(5));
  }

  @Override
  public ConsumerBuilder<T> withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Override
  public KafkaConsumer<T> start() {
    ConfigProvider configProvider = consumerFactory.getConfigProvider();
    ConsumerFactory<String, T> springConsumerFactory = consumerFactory.getSpringConsumerFactory(topicName, messageClass);
    ConsumerMetadata consumerMetadata = new ConsumerMetadata(configProvider.getServiceName(), topicName, operationName);
    if (useConsumerGroup) {
      RetryService<T> retryService = null;
      KafkaConsumer<T> retryKafkaConsumer = null;
      if (usingRetries()) {
        retryService = new RetryService<>(retryProducer, getRetrySendTopicName(), retryPolicyResolver);
        retryKafkaConsumer = buildAndStartRetryKafkaConsumer(retryService);
      }
      return startKafkaConsumerForConsumerGroup(
          configProvider,
          springConsumerFactory,
          consumerMetadata,
          retryService,
          retryKafkaConsumer
      );
    }
    return startKafkaConsumerForAllPartitions(configProvider, springConsumerFactory, consumerMetadata);
  }

  private boolean usingRetries() {
    return retryProducer != null && retryPolicyResolver != null;
  }

  private String getRetryReceiveTopicName() {
    return topicName + "_" + operationName + "_retry_receive";
  }

  private String getRetrySendTopicName() {
    return standaloneRetries ? getRetryReceiveTopicName() : topicName + "_" + operationName + "_retry_send";
  }

  private KafkaConsumer<T> startKafkaConsumerForConsumerGroup(
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

    KafkaConsumer<T> kafkaConsumer = new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, consumeStrategy),
        retryService,
        retryKafkaConsumer,
        springContainerProvider,
        ackProvider
    );
    kafkaConsumer.start();
    logger.info("Subscribed for {}, consumer group id {}", topicName, consumerMetadata.getConsumerGroupId());
    return kafkaConsumer;
  }

  private KafkaConsumer<T> buildAndStartRetryKafkaConsumer(RetryService<T> retryService) {
    ConfigProvider configProvider = consumerFactory.getConfigProvider();
    String retryReceiveTopicName = getRetryReceiveTopicName();
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
    ConsumeStrategy<T> retryReceiveConsumeStrategy = new SimpleDelayedConsumeStrategy<>(
        consumeStrategy,
        message -> HeadersMessageMetadataProvider
            .getNextRetryTime(message.headers())
            .orElse(Instant.EPOCH),
        Duration.ofSeconds(10)
    );
    KafkaConsumer<T> retryKafkaConsumer = new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, retryReceiveConsumeStrategy),
        retryService,
        null, // retry consumer has no retry consumer
        springContainerProvider,
        ackProvider
    );
    retryKafkaConsumer.start();
    return retryKafkaConsumer;
  }

  private KafkaConsumer<T> startKafkaConsumerForAllPartitions(
      ConfigProvider configProvider, ConsumerFactory<String, T> springConsumerFactory, ConsumerMetadata consumerMetadata
  ) {
    if (retryProducer != null || retryPolicyResolver != null) {
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

    KafkaConsumer<T> kafkaConsumer = new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, consumeStrategy),
        springContainerProvider,
        consumerFactory.getTopicPartitionsMonitoring(),
        consumerFactory.getClusterMetadataProvider(),
        ackProvider,
        checkNewPartitionsInterval
    );
    kafkaConsumer.start();
    logger.info("Subscribed {} for all partitions, operation={}", topicName, consumerMetadata.getOperation());
    return kafkaConsumer;
  }


  private ConcurrentMessageListenerContainer<String, T> getSpringKafkaListenerContainer(
      ConfigProvider configProvider, ConsumerFactory<String, T> springConsumerFactory, KafkaConsumer<T> nabKafkaConsumer,
      ContainerProperties containerProperties
  ) {
    SeekToFirstNotAckedMessageErrorHandler<T> errorHandler = consumerFactory.getCommonErrorHandler(topicName, nabKafkaConsumer, logger);
    ConcurrentMessageListenerContainer<String, T> container = new ConcurrentMessageListenerContainer<>(springConsumerFactory, containerProperties);
    container.setCommonErrorHandler(errorHandler);
    container.setConcurrency(configProvider
        .getNabConsumerSettings(topicName)
        .getInteger(CONCURRENCY, 1));
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
    containerProperties.setClientId(Optional
        .ofNullable(clientId)
        .orElseGet(() -> UUID
            .randomUUID()
            .toString()));
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    containerProperties.setPollTimeout(nabConsumerSettings.getLong(POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS));
    containerProperties.setAuthExceptionRetryInterval(Duration.ofMillis(nabConsumerSettings.getLong(
        AUTH_EXCEPTION_RETRY_INTERVAL,
        DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS
    )));
  }

}
