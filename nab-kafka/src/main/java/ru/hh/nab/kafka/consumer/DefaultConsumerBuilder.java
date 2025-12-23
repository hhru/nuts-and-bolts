package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.exception.ConfigurationException;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.AUTH_EXCEPTION_RETRY_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.CONCURRENCY;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_GROUP_STATIC_MEMBERSHIP_ENABLED;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_POLL_TIMEOUT_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_SHUTDOWN_TIMEOUT_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.GROUP_STATIC_MEMBERSHIP_ENABLED;
import static ru.hh.nab.kafka.util.ConfigProvider.POLL_TIMEOUT;
import static ru.hh.nab.kafka.util.ConfigProvider.SHUTDOWN_TIMEOUT_MS;
import ru.hh.nab.metrics.executor.MonitoredThreadPoolExecutor;

public class DefaultConsumerBuilder<T> implements ConsumerBuilder<T> {

  private static final String KAFKA_CONSUMER_THREAD_POOL_NAME_TEMPLATE = "kafka-consumer-%s%s-%s";
  private static final String KAFKA_MESSAGE_PROCESSOR_THREAD_POOL_NAME_TEMPLATE = "kafka-message-processor-%s%s-%s";

  private final String topicName;
  private final Class<T> messageClass;
  private final DefaultConsumerFactory consumerFactory;
  private String operationName;
  private String clientId;
  private boolean isConsumerGroup;
  private SeekPosition seekPositionIfNoConsumerGroup;
  private Duration checkNewPartitionsInterval;

  private ConsumeStrategy<T> consumeStrategy;
  private RetryPolicyResolver<T> retryPolicyResolver;
  private KafkaProducer retryProducer;
  private RetryTopics retryTopics;
  private ConsumeStrategy<T> retryConsumeStrategy;
  private Logger logger;
  private AckProvider<T> ackProvider;

  private String deadLetterQueueDesination;
  private KafkaProducer deadLetterQueueProducer;

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
  public ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver) {
    return withRetries(retryProducer, retryPolicyResolver, RetryTopics.DEFAULT_SINGLE_TOPIC);
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
  public ConsumerBuilder<T> withDlq(String destination, KafkaProducer producer) {
    this.deadLetterQueueDesination = destination;
    this.deadLetterQueueProducer = producer;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withConsumerGroup() {
    this.isConsumerGroup = true;
    this.ackProvider = KafkaInternalTopicAck::new;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition, Duration checkNewPartitionsInterval) {
    this.isConsumerGroup = false;
    this.seekPositionIfNoConsumerGroup = seekPosition;
    this.ackProvider = (kafkaConsumer, nativeKafkaConsumer) -> new InMemorySeekOnlyAck<>(kafkaConsumer);
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
    if (topicName == null) {
      throw new ConfigurationException("Messages can not be consumed without specified topic. See DefaultConsumerBuilder constructor");
    }

    if (messageClass == null) {
      throw new ConfigurationException("Messages without specified class can not be consumed. See DefaultConsumerBuilder constructor");
    }

    if (consumeStrategy == null) {
      throw new ConfigurationException("Messages can not be consumed without strategy. See ConsumerBuilder#withConsumeStrategy");
    }

    if (usingRetries() && !isConsumerGroup) {
      throw new ConfigurationException("Consumers without group id are not supported for retries.");
    }

    ConfigProvider configProvider = consumerFactory.getConfigProvider();
    ConsumerFactory<String, T> springConsumerFactory = consumerFactory.getSpringConsumerFactory(topicName, messageClass);
    ConsumerMetadata consumerMetadata = new ConsumerMetadata(configProvider.getServiceName(), configProvider.getNodeName(), topicName, operationName);

    DeadLetterQueue<T> deadLetterQueue = null;
    if (usingDlq()) {
      deadLetterQueue = new DeadLetterQueue<>(this.deadLetterQueueDesination, deadLetterQueueProducer);
    }

    return isConsumerGroup ?
        buildKafkaConsumerForConsumerGroup(configProvider, springConsumerFactory, consumerMetadata, deadLetterQueue)
        :
        buildKafkaConsumerForAllPartitions(configProvider, springConsumerFactory, consumerMetadata, deadLetterQueue);
  }

  private boolean usingDlq() {
    return deadLetterQueueDesination != null;
  }

  private boolean usingRetries() {
    return retryPolicyResolver != null;
  }

  private KafkaConsumer<T> buildKafkaConsumerForConsumerGroup(
      ConfigProvider configProvider,
      ConsumerFactory<String, T> springConsumerFactory,
      ConsumerMetadata consumerMetadata,
      DeadLetterQueue<T> deadLetterQueue
  ) {
    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = createSpringContainerProviderForConsumerGroup(
        configProvider,
        springConsumerFactory,
        consumerMetadata
    );

    RetryQueue<T> retryQueue = null;
    KafkaConsumer<T> retryKafkaConsumer = null;
    if (usingRetries()) {
      if (retryTopics == null) {
        throw new ConfigurationException("Retries can not be used without retry topics. See ConsumerBuilder#withRetries");
      }
      retryTopics = normalizeTopics(consumerMetadata);
      retryQueue = new RetryQueue<>(deadLetterQueue, retryProducer, retryTopics, retryPolicyResolver);
      retryKafkaConsumer = buildRetryKafkaConsumer(retryQueue, deadLetterQueue);
    }

    return new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, consumeStrategy),
        retryQueue,
        retryKafkaConsumer,
        deadLetterQueue,
        springContainerProvider,
        ackProvider,
        logger
    );
  }

  private RetryTopics normalizeTopics(ConsumerMetadata consumerMetadata) {
    if (RetryTopics.DEFAULT_SINGLE_TOPIC == retryTopics) {
      return RetryTopics.defaultSingleTopic(consumerMetadata);
    } else if (RetryTopics.DEFAULT_PAIR_OF_TOPICS == retryTopics) {
      return RetryTopics.defaultPairOfTopics(consumerMetadata);
    } else {
      return retryTopics;
    }
  }

  private KafkaConsumer<T> buildRetryKafkaConsumer(RetryQueue<T> retryQueue, DeadLetterQueue<T> deadLetterQueue) {
    ConfigProvider configProvider = consumerFactory.getConfigProvider();
    String retryReceiveTopicName = retryTopics.retryReceiveTopic();
    ConsumerFactory<String, T> springConsumerFactory = consumerFactory.getSpringConsumerFactory(retryReceiveTopicName, messageClass);
    ConsumerMetadata consumerMetadata = new ConsumerMetadata(
        configProvider.getServiceName(),
        configProvider.getNodeName(),
        retryReceiveTopicName,
        ""
    );
    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = createSpringContainerProviderForConsumerGroup(
        configProvider,
        springConsumerFactory,
        consumerMetadata
    );
    ConsumeStrategy<T> retryReceiveConsumeStrategy = getRetryReceiveConsumeStrategy();
    return new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, retryReceiveConsumeStrategy),
        retryQueue,
        null, // retry consumer has no retry consumer
        deadLetterQueue,
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
    Properties settings = consumerFactory.configProvider.getNabConsumerSettings(topicName);
    long pollTimeout = PropertiesUtils.getLong(settings, POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS);
    return ConsumerBuilder.decorateForDelayedRetry(consumeStrategy, Duration.ofMillis(pollTimeout * 9 / 10));
  }

  private Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> createSpringContainerProviderForConsumerGroup(
      ConfigProvider configProvider,
      ConsumerFactory<String, T> springConsumerFactory,
      ConsumerMetadata consumerMetadata
  ) {
    Properties nabConsumerSettings = configProvider.getNabConsumerSettings(consumerMetadata.getTopic());
    int concurrency = getConcurrency(nabConsumerSettings);
    AtomicInteger containersCount = new AtomicInteger(0);
    return (nabKafkaConsumer) -> {
      int containerIndex = containersCount.getAndIncrement();
      ThreadPoolExecutor messageProcessingExecutor = MonitoredThreadPoolExecutor.create(
          concurrency,
          0,
          getMessageProcessingThreadPoolName(consumerMetadata, containerIndex),
          consumerFactory.getStatsDSender(),
          configProvider.getServiceName()
      );
      ContainerProperties containerProperties = getSpringConsumerContainerPropertiesWithConsumerGroup(
          nabConsumerSettings,
          consumerMetadata,
          getMessageListener(nabKafkaConsumer, messageProcessingExecutor)
      );
      return getSpringKafkaListenerContainer(
          springConsumerFactory,
          nabKafkaConsumer,
          containerProperties,
          messageProcessingExecutor,
          getKafkaConsumerThreadPoolName(consumerMetadata, containerIndex)
      );
    };
  }

  private KafkaConsumer<T> buildKafkaConsumerForAllPartitions(
      ConfigProvider configProvider, ConsumerFactory<String, T> springConsumerFactory, ConsumerMetadata consumerMetadata,
      DeadLetterQueue<T> deadLetterQueue
  ) {
    Properties nabConsumerSettings = configProvider.getNabConsumerSettings(consumerMetadata.getTopic());
    int concurrency = getConcurrency(nabConsumerSettings);
    AtomicInteger containersCount = new AtomicInteger(0);
    BiFunction<KafkaConsumer<T>, List<PartitionInfo>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (
        nabKafkaConsumer,
        partitionsInfo
    ) -> {
      int containerIndex = containersCount.getAndIncrement();
      ThreadPoolExecutor messageProcessingExecutor = MonitoredThreadPoolExecutor.create(
          // When specific partitions are provided, the pool size must be less than or equal to the number of partitions
          Math.min(concurrency, partitionsInfo.size()),
          0,
          getMessageProcessingThreadPoolName(consumerMetadata, containerIndex),
          consumerFactory.getStatsDSender(),
          configProvider.getServiceName()
      );
      ContainerProperties containerProperties = getSpringConsumerContainerPropertiesSubscribedToAllPartitions(
          nabConsumerSettings,
          getMessageListener(nabKafkaConsumer, messageProcessingExecutor),
          partitionsInfo,
          nabKafkaConsumer
      );
      return getSpringKafkaListenerContainer(
          springConsumerFactory,
          nabKafkaConsumer,
          containerProperties,
          messageProcessingExecutor,
          getKafkaConsumerThreadPoolName(consumerMetadata, containerIndex)
      );
    };

    return new KafkaConsumer<>(
        consumerMetadata,
        consumerFactory.interceptConsumeStrategy(consumerMetadata, consumeStrategy),
        deadLetterQueue,
        springContainerProvider,
        consumerFactory.getTopicPartitionsMonitoring(),
        consumerFactory.getClusterMetadataProvider(),
        ackProvider,
        logger,
        checkNewPartitionsInterval
    );
  }


  private ConcurrentMessageListenerContainer<String, T> getSpringKafkaListenerContainer(
      ConsumerFactory<String, T> springConsumerFactory,
      KafkaConsumer<T> nabKafkaConsumer,
      ContainerProperties containerProperties,
      ThreadPoolExecutor messageProcessingExecutor,
      String kafkaConsumerThreadPoolName
  ) {
    SeekToFirstNotAckedMessageErrorHandler<T> errorHandler = consumerFactory.getCommonErrorHandler(
        topicName,
        nabKafkaConsumer,
        logger
    );
    NabConcurrentMessageListenerContainer<String, T> container = new NabConcurrentMessageListenerContainer<>(
        springConsumerFactory,
        containerProperties,
        messageProcessingExecutor,
        logger
    );
    container.setBeanName(kafkaConsumerThreadPoolName);
    container.setCommonErrorHandler(errorHandler);
    container.setConcurrency(messageProcessingExecutor.getMaximumPoolSize());
    return container;
  }

  private ContainerProperties getSpringConsumerContainerPropertiesSubscribedToAllPartitions(
      Properties nabConsumerSettings,
      GenericMessageListener<?> messageListener,
      List<PartitionInfo> partitionsInfo,
      KafkaConsumer<T> nabKafkaConsumer
  ) {
    ConsumerContext<T> consumerContext = nabKafkaConsumer.getConsumerContext();
    TopicPartitionOffset[] partitions = partitionsInfo
        .stream()
        .map(partition -> consumerContext
            .getGlobalSeekedOffset(new TopicPartition(topicName, partition.partition()))
            .map(seekedOffset -> new TopicPartitionOffset(topicName, partition.partition(), seekedOffset.offset()))
            .orElseGet(() -> new TopicPartitionOffset(topicName, partition.partition(), seekPositionIfNoConsumerGroup.getSpringSeekPosition()))
        )
        .toArray(TopicPartitionOffset[]::new);

    var containerProperties = new ContainerProperties(partitions);
    addCommonContainerProperties(messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private ContainerProperties getSpringConsumerContainerPropertiesWithConsumerGroup(
      Properties nabConsumerSettings,
      ConsumerMetadata consumerMetadata,
      GenericMessageListener<?> messageListener
  ) {
    var containerProperties = new ContainerProperties(consumerMetadata.getTopic());
    containerProperties.setGroupId(consumerMetadata.getConsumerGroupId());
    if (PropertiesUtils.getBoolean(nabConsumerSettings, GROUP_STATIC_MEMBERSHIP_ENABLED, DEFAULT_GROUP_STATIC_MEMBERSHIP_ENABLED)) {
      containerProperties
          .getKafkaConsumerProperties()
          .put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, consumerMetadata.getConsumerInstanceId());
    }
    addCommonContainerProperties(messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private void addCommonContainerProperties(
      GenericMessageListener<?> messageListener, ContainerProperties containerProperties, Properties settings
  ) {
    containerProperties.setClientId(Optional.ofNullable(clientId).orElseGet(() -> UUID.randomUUID().toString()));
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    containerProperties.setPollTimeout(PropertiesUtils.getLong(settings, POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS));
    containerProperties.setAuthExceptionRetryInterval(Duration.ofMillis(PropertiesUtils.getLong(
        settings,
        AUTH_EXCEPTION_RETRY_INTERVAL,
        DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS
    )));
    containerProperties.setShutdownTimeout(PropertiesUtils.getLong(settings, SHUTDOWN_TIMEOUT_MS, DEFAULT_SHUTDOWN_TIMEOUT_MS));
  }

  private int getConcurrency(Properties nabConsumerSettings) {
    return PropertiesUtils.getInteger(nabConsumerSettings, CONCURRENCY, 1);
  }

  private BatchConsumerAwareMessageListener<String, T> getMessageListener(
      KafkaConsumer<T> kafkaConsumer,
      ThreadPoolExecutor messageProcessingExecutor
  ) {
    return (messages, consumer) -> kafkaConsumer.onMessagesBatch(messages, consumer, messageProcessingExecutor);
  }

  private String getKafkaConsumerThreadPoolName(ConsumerMetadata consumerMetadata, int containerIndex) {
    return getThreadPoolName(KAFKA_CONSUMER_THREAD_POOL_NAME_TEMPLATE, consumerMetadata, containerIndex);
  }

  private String getMessageProcessingThreadPoolName(ConsumerMetadata consumerMetadata, int containerIndex) {
    return getThreadPoolName(KAFKA_MESSAGE_PROCESSOR_THREAD_POOL_NAME_TEMPLATE, consumerMetadata, containerIndex);
  }

  private String getThreadPoolName(String threadPoolNameTemplate, ConsumerMetadata consumerMetadata, int containerIndex) {
    return threadPoolNameTemplate.formatted(
        consumerMetadata.getTopic(),
        consumerMetadata.getOperation().isBlank() ? "" : "-" + consumerMetadata.getOperation(),
        containerIndex
    );
  }
}
