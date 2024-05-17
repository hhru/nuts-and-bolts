package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.Consumer;
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
  public ConsumerBuilder<T> withAllPartitionsAssigned(TopicPartitionOffset.SeekPosition seekPosition) {
    this.useConsumerGroup = false;
    this.seekPositionIfNoConsumerGroup = seekPosition;
    withAckProvider((kafkaConsumer, nativeKafkaConsumer) -> new InMemorySeekOnlyAck<>(kafkaConsumer));
    return this;
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

    ConsumerGroupId consumerGroupId = new ConsumerGroupId(configProvider.getServiceName(), topicName, operationName);

    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (kafkaConsumer) -> {
      ContainerProperties containerProperties = getContainerProperties(
          configProvider,
          Optional.ofNullable(clientId).orElseGet(() -> UUID.randomUUID().toString()),
          consumerGroupId,
          (BatchConsumerAwareMessageListener<String, T>) kafkaConsumer::onMessagesBatch,
          topicName,
          springConsumerFactory
      );
      SeekToFirstNotAckedMessageErrorHandler<T> errorHandler = consumerFactory.getCommonErrorHandler(topicName, kafkaConsumer, logger);

      ConcurrentMessageListenerContainer<String, T> container = new ConcurrentMessageListenerContainer<>(springConsumerFactory, containerProperties);
      container.setCommonErrorHandler(errorHandler);
      container.setConcurrency(configProvider.getNabConsumerSettings(topicName).getInteger(CONCURRENCY, 1));
      return container;
    };

    KafkaConsumer<T> kafkaConsumer = new KafkaConsumer<>(
        consumerFactory.interceptConsumeStrategy(consumerGroupId, consumeStrategy),
        springContainerProvider,
        ackProvider
    );
    kafkaConsumer.start();
    logger.info("Subscribed for {}, consumer group id {}", topicName, consumerGroupId);
    return kafkaConsumer;
  }

  private ContainerProperties getContainerProperties(
      ConfigProvider configProvider,
      String clientId,
      ConsumerGroupId consumerGroupId,
      GenericMessageListener<?> messageListener,
      String topicName,
      ConsumerFactory<String, T> springConsumerFactory
  ) {
    if (useConsumerGroup) {
      return getSpringConsumerContainerPropertiesWithConsumerGroup(
          configProvider,
          clientId,
          consumerGroupId,
          messageListener,
          topicName
      );
    }
    return getSpringConsumerContainerPropertiesSubscribedToAllPartitions(
        configProvider,
        clientId,
        consumerGroupId,
        messageListener,
        topicName,
        springConsumerFactory
    );

  }

  private ContainerProperties getSpringConsumerContainerPropertiesSubscribedToAllPartitions(
      ConfigProvider configProvider,
      String clientId,
      ConsumerGroupId consumerGroupId,
      GenericMessageListener<?> messageListener,
      String topicName,
      ConsumerFactory<String, T> springConsumerFactory
  ) {
    TopicPartitionOffset[] partitions;
    try (Consumer<String, T> consumer = springConsumerFactory.createConsumer()) {
      partitions = consumer
          .partitionsFor(topicName)
          .stream()
          .map((partition) -> new TopicPartitionOffset(partition.topic(), partition.partition(), this.seekPositionIfNoConsumerGroup))
          .toArray(TopicPartitionOffset[]::new);
    }

    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(topicName);
    var containerProperties = new ContainerProperties(partitions);
    addCommonContainerProperties(clientId, messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private ContainerProperties getSpringConsumerContainerPropertiesWithConsumerGroup(
      ConfigProvider configProvider,
      String clientId,
      ConsumerGroupId consumerGroupId,
      GenericMessageListener<?> messageListener,
      String topicName
  ) {
    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(topicName);
    var containerProperties = new ContainerProperties(consumerGroupId.getTopic());
    containerProperties.setGroupId(consumerGroupId.toString());
    addCommonContainerProperties(clientId, messageListener, containerProperties, nabConsumerSettings);
    return containerProperties;
  }

  private static void addCommonContainerProperties(
      String clientId, GenericMessageListener<?> messageListener, ContainerProperties containerProperties, FileSettings nabConsumerSettings
  ) {
    containerProperties.setClientId(clientId);
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    containerProperties.setPollTimeout(nabConsumerSettings.getLong(POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS));
    containerProperties.setAuthExceptionRetryInterval(
        Duration.ofMillis(nabConsumerSettings.getLong(AUTH_EXCEPTION_RETRY_INTERVAL, DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS))
    );
  }

}
