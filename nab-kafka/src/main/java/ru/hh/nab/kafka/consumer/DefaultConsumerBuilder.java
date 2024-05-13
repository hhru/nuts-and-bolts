package ru.hh.nab.kafka.consumer;

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
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.CONCURRENCY;

public class DefaultConsumerBuilder<T> implements ConsumerBuilder<T> {

  private final String topicName;
  private final Class<T> messageClass;
  private final DefaultConsumerFactory consumerFactory;
  private String operationName;
  private String clientId;
  private boolean useConsumerGroup = true; // FALSE value not supported now
  private ConsumeStrategy<T> consumeStrategy;
  private Logger logger;
  private BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider;

  public DefaultConsumerBuilder(DefaultConsumerFactory consumerFactory, String topicName, Class<T> messageClass) {
    this.topicName = topicName;
    this.messageClass = messageClass;
    this.consumerFactory = consumerFactory;

    withAckProvider((kafkaConsumer, nativeKafkaConsumer) -> new KafkaInternalTopicAck<>(kafkaConsumer, nativeKafkaConsumer));
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
  public ConsumerBuilder<T> withUseConsumerGroup(boolean useConsumerGroup) {
    this.useConsumerGroup = useConsumerGroup;
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
      ContainerProperties containerProperties = consumerFactory.getSpringConsumerContainerProperties(
          Optional.ofNullable(clientId).orElseGet(() -> UUID.randomUUID().toString()),
          consumerGroupId,
          (BatchConsumerAwareMessageListener<String, T>) kafkaConsumer::onMessagesBatch,
          topicName
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

}
