package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.Consumer;
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
  private TopicPartitionOffset.SeekPosition seekPositionIfNoConsumerGroup;
  private Duration checkNewPartitionsInterval;

  private ConsumeStrategy<T> consumeStrategy;
  private Logger logger;
  private BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider;

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
  public DefaultConsumerBuilder<T> withLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  @Override
  public DefaultConsumerBuilder<T> withAckProvider(BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider) {
    this.ackProvider = ackProvider;
    return this;
  }

  @Override
  public ConsumerBuilder<T> withConsumerGroup() {
    this.useConsumerGroup = true;
    withAckProvider((kafkaConsumer, nativeKafkaConsumer) -> new KafkaInternalTopicAck<>(kafkaConsumer, nativeKafkaConsumer));
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAllPartitionsAssigned(TopicPartitionOffset.SeekPosition seekPosition, Duration checkNewPartitionsInterval) {
    this.useConsumerGroup = false;
    this.seekPositionIfNoConsumerGroup = seekPosition;
    withAckProvider((kafkaConsumer, nativeKafkaConsumer) -> new InMemorySeekOnlyAck<>(kafkaConsumer));
    this.checkNewPartitionsInterval = checkNewPartitionsInterval;
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAllPartitionsAssigned(TopicPartitionOffset.SeekPosition seekPosition) {
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
    ConsumerDescription consumerDescription = new ConsumerDescription(configProvider.getServiceName(), topicName, operationName);
    if (useConsumerGroup) {
      return startKafkaConsumerForConsumerGroup(configProvider, springConsumerFactory, consumerDescription);
    }
    return startKafkaConsumerForAllPartitions(configProvider, springConsumerFactory, consumerDescription);

  }

  private KafkaConsumer<T> startKafkaConsumerForConsumerGroup(
      ConfigProvider configProvider,
      ConsumerFactory<String, T> springConsumerFactory,
      ConsumerDescription consumerDescription
  ) {
    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (nabKafkaConsumer) -> {
      ContainerProperties containerProperties = getSpringConsumerContainerPropertiesWithConsumerGroup(
          configProvider,
          consumerDescription,
          (BatchConsumerAwareMessageListener<String, T>) nabKafkaConsumer::onMessagesBatch
      );
      return getSpringKafkaListenerContainer(configProvider, springConsumerFactory, nabKafkaConsumer, containerProperties);
    };

    KafkaConsumer<T> kafkaConsumer = new KafkaConsumer<>(
        consumerDescription,
        consumerFactory.interceptConsumeStrategy(consumerDescription, consumeStrategy),
        springContainerProvider,
        ackProvider
    );
    kafkaConsumer.start();
    logger.info("Subscribed for {}, consumer group id {}", topicName, consumerDescription.getConsumerGroupId());
    return kafkaConsumer;
  }

  private KafkaConsumer<T> startKafkaConsumerForAllPartitions(
      ConfigProvider configProvider, ConsumerFactory<String, T> springConsumerFactory, ConsumerDescription consumerDescription
  ) {

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
        consumerDescription,
        consumerFactory.interceptConsumeStrategy(consumerDescription, consumeStrategy),
        springContainerProvider,
        consumerFactory.getTopicPartitionsMonitoring(),
        consumerFactory.getClusterMetaInfoProvider(),
        ackProvider,
        checkNewPartitionsInterval
    );
    kafkaConsumer.start();
    logger.info("Subscribed {} for all partitions, operation={}", topicName, consumerDescription.getOperation());
    return kafkaConsumer;
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
            .orElseGet(() -> new TopicPartitionOffset(topicName, partition.partition(), seekPositionIfNoConsumerGroup))
        )
        .toArray(TopicPartitionOffset[]::new);

    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(topicName);
    var containerProperties = new ContainerProperties(partitions);
    addCommonContainerProperties(messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private ContainerProperties getSpringConsumerContainerPropertiesWithConsumerGroup(
      ConfigProvider configProvider, ConsumerDescription consumerDescription, GenericMessageListener<?> messageListener
  ) {
    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(topicName);
    var containerProperties = new ContainerProperties(topicName);
    containerProperties.setGroupId(consumerDescription.getConsumerGroupId());
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
