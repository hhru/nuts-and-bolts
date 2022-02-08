package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.BatchConsumerAwareMessageListener;
import org.springframework.kafka.listener.BatchErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.util.backoff.ExponentialBackOff;
import ru.hh.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.monitoring.MonitoringConsumeStrategy;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.AUTH_EXCEPTION_RETRY_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.BACKOFF_INITIAL_INTERVAL_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.BACKOFF_MAX_INTERVAL_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.BACKOFF_MULTIPLIER_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_BACKOFF_INITIAL_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_BACKOFF_MAX_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_BACKOFF_MULTIPLIER;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_POLL_TIMEOUT_MS;
import static ru.hh.nab.kafka.util.ConfigProvider.POLL_TIMEOUT;
import ru.hh.nab.metrics.StatsDSender;

public class DefaultConsumerFactory implements KafkaConsumerFactory {
  protected final ConfigProvider configProvider;
  private final DeserializerSupplier deserializerSupplier;
  private final StatsDSender statsDSender;
  private final Logger factoryLogger;

  public DefaultConsumerFactory(ConfigProvider configProvider,
                                DeserializerSupplier deserializerSupplier,
                                StatsDSender statsDSender,
                                Logger logger) {
    this.configProvider = configProvider;
    this.deserializerSupplier = deserializerSupplier;
    this.statsDSender = statsDSender;
    this.factoryLogger = logger;
  }

  public DefaultConsumerFactory(ConfigProvider configProvider,
                                DeserializerSupplier deserializerSupplier,
                                StatsDSender statsDSender) {
    this(
        configProvider,
        deserializerSupplier,
        statsDSender,
        LoggerFactory.getLogger(DefaultConsumerFactory.class)
    );
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(String topicName,
                                        String operationName,
                                        Class<T> messageClass,
                                        ConsumeStrategy<T> consumeStrategy) {
    return subscribe(topicName, operationName, messageClass, consumeStrategy, this.factoryLogger);
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(String topicName,
                                        String operationName,
                                        Class<T> messageClass,
                                        ConsumeStrategy<T> consumeStrategy,
                                        Logger logger) {
    ConsumerFactory<String, T> consumerFactory = getSpringConsumerFactory(topicName, messageClass);
    ConsumerGroupId consumerGroupId = new ConsumerGroupId(configProvider.getServiceName(), topicName, operationName);

    Function<KafkaConsumer<T>, AbstractMessageListenerContainer<String, T>> springContainerProvider = (kafkaConsumer) -> {
      ContainerProperties containerProperties = getSpringConsumerContainerProperties(
          consumerGroupId,
          (BatchConsumerAwareMessageListener<String, T>) kafkaConsumer::onMessagesBatch,
          topicName
      );
      SeekToFirstNotAckedMessageErrorHandler<T> errorHandler = getBatchErrorHandler(topicName, kafkaConsumer, logger);
      return getSpringMessageListenerContainer(consumerFactory, containerProperties, errorHandler);
    };

    KafkaConsumer<T> kafkaConsumer = new KafkaConsumer<>(prepare(consumerGroupId, consumeStrategy), springContainerProvider);

    kafkaConsumer.start();
    return kafkaConsumer;
  }

  protected  <T> ConsumeStrategy<T> prepare(ConsumerGroupId consumerGroupId, ConsumeStrategy<T> consumeStrategy) {
    return new MonitoringConsumeStrategy<>(statsDSender, consumerGroupId, consumeStrategy);
  }

  public static <T> ConcurrentMessageListenerContainer<String, T> getSpringMessageListenerContainer(ConsumerFactory<String, T> consumerFactory,
                                                                                                    ContainerProperties containerProperties,
                                                                                                    BatchErrorHandler errorHandler) {
    var container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    container.setBatchErrorHandler(errorHandler);
    return container;
  }

  private <T> SeekToFirstNotAckedMessageErrorHandler<T> getBatchErrorHandler(String topicName, KafkaConsumer<T> kafkaConsumer, Logger logger) {
    FileSettings settings = configProvider.getNabConsumerSettings(topicName);
    ExponentialBackOff backOff = new ExponentialBackOff(
        settings.getLong(BACKOFF_INITIAL_INTERVAL_NAME, DEFAULT_BACKOFF_INITIAL_INTERVAL),
        settings.getDouble(BACKOFF_MULTIPLIER_NAME, DEFAULT_BACKOFF_MULTIPLIER)
    );
    backOff.setMaxInterval(settings.getLong(BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL));
    return new SeekToFirstNotAckedMessageErrorHandler<>(logger, backOff, kafkaConsumer);
  }

  private <T> ConsumerFactory<String, T> getSpringConsumerFactory(String topicName, Class<T> messageClass) {
    Map<String, Object> consumerConfig = configProvider.getConsumerConfig(topicName);
    consumerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    return new FailFastDefaultKafkaConsumerFactory<>(
        topicName,
        consumerConfig,
        new StringDeserializer(),
        deserializerSupplier.supplyFor(messageClass)
    );
  }

  private ContainerProperties getSpringConsumerContainerProperties(ConsumerGroupId consumerGroupId,
                                                                   GenericMessageListener<?> messageListener,
                                                                   String topicName) {
    FileSettings nabConsumerSettings = configProvider.getNabConsumerSettings(topicName);
    var containerProperties = new ContainerProperties(consumerGroupId.getTopic());
    containerProperties.setGroupId(consumerGroupId.toString());
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    containerProperties.setPollTimeout(nabConsumerSettings.getLong(POLL_TIMEOUT, DEFAULT_POLL_TIMEOUT_MS));
    containerProperties.setAuthorizationExceptionRetryInterval(
        Duration.ofMillis(nabConsumerSettings.getLong(AUTH_EXCEPTION_RETRY_INTERVAL, DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS)));
    return containerProperties;
  }
}
