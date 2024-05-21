package ru.hh.nab.kafka.consumer;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.backoff.ExponentialBackOff;
import ru.hh.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.monitoring.MonitoringConsumeStrategy;
import ru.hh.nab.kafka.util.ConfigProvider;
import static ru.hh.nab.kafka.util.ConfigProvider.BACKOFF_INITIAL_INTERVAL_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.BACKOFF_MAX_INTERVAL_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.BACKOFF_MULTIPLIER_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_BACKOFF_INITIAL_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_BACKOFF_MAX_INTERVAL;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_BACKOFF_MULTIPLIER;
import ru.hh.nab.metrics.StatsDSender;

public class DefaultConsumerFactory implements KafkaConsumerFactory {
  protected final ConfigProvider configProvider;
  private final DeserializerSupplier deserializerSupplier;
  private final StatsDSender statsDSender;
  private final Logger factoryLogger;
  private final Supplier<String> bootstrapServersSupplier;
  private final ClusterMetaInfoProvider clusterMetaInfoProvider;
  private final TopicPartitionsMonitoring topicPartitionsMonitoring;

  public DefaultConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      Logger logger
  ) {
    this(configProvider, deserializerSupplier, statsDSender, logger, null);
  }

  public DefaultConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender
  ) {
    this(configProvider, deserializerSupplier, statsDSender, LoggerFactory.getLogger(DefaultConsumerFactory.class), null);
  }

  public DefaultConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      @Nullable Supplier<String> bootstrapServersSupplier
  ) {
    this(configProvider, deserializerSupplier, statsDSender, LoggerFactory.getLogger(DefaultConsumerFactory.class), bootstrapServersSupplier);
  }

  public DefaultConsumerFactory(
      ConfigProvider configProvider,
      DeserializerSupplier deserializerSupplier,
      StatsDSender statsDSender,
      Logger logger,
      @Nullable Supplier<String> bootstrapServersSupplier
  ) {
    this.configProvider = configProvider;
    this.deserializerSupplier = deserializerSupplier;
    this.statsDSender = statsDSender;
    this.factoryLogger = logger;
    this.bootstrapServersSupplier = bootstrapServersSupplier;
    this.clusterMetaInfoProvider = new ClusterMetaInfoProvider(this);
    this.topicPartitionsMonitoring = new TopicPartitionsMonitoring(this.clusterMetaInfoProvider);
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(String topicName, String operationName, Class<T> messageClass, ConsumeStrategy<T> consumeStrategy) {
    return builder(topicName, messageClass)
        .withOperationName(operationName)
        .withConsumeStrategy(consumeStrategy)
        .start();
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(
      String topicName,
      String operationName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumeStrategy,
      Logger logger
  ) {
    return builder(topicName, messageClass)
        .withLogger(logger)
        .withOperationName(operationName)
        .withConsumeStrategy(consumeStrategy)
        .start();
  }

  @Override
  public <T> KafkaConsumer<T> subscribe(
      String clientId, String topicName, String operationName, Class<T> messageClass, ConsumeStrategy<T> consumeStrategy, Logger logger
  ) {
    return builder(topicName, messageClass)
        .withLogger(logger)
        .withOperationName(operationName)
        .withConsumeStrategy(consumeStrategy)
        .withClientId(clientId)
        .start();
  }

  @Override
  public <T> ConsumerBuilder<T> builder(String topicName, Class<T> messageClass) {
    return new DefaultConsumerBuilder<>(this, topicName, messageClass)
        .withLogger(factoryLogger);
  }


  public <T> ConsumeStrategy<T> interceptConsumeStrategy(ConsumerDescription consumerDescription, ConsumeStrategy<T> consumeStrategy) {
    return new MonitoringConsumeStrategy<>(statsDSender, consumerDescription, consumeStrategy);
  }

  <T> SeekToFirstNotAckedMessageErrorHandler<T> getCommonErrorHandler(String topicName, KafkaConsumer<T> kafkaConsumer, Logger logger) {
    FileSettings settings = configProvider.getNabConsumerSettings(topicName);
    ExponentialBackOff backOff = new ExponentialBackOff(
        settings.getLong(BACKOFF_INITIAL_INTERVAL_NAME, DEFAULT_BACKOFF_INITIAL_INTERVAL),
        settings.getDouble(BACKOFF_MULTIPLIER_NAME, DEFAULT_BACKOFF_MULTIPLIER)
    );
    backOff.setMaxInterval(settings.getLong(BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL));
    return new SeekToFirstNotAckedMessageErrorHandler<>(logger, backOff, kafkaConsumer);
  }

  <T> ConsumerFactory<String, T> getSpringConsumerFactory(String topicName, Class<T> messageClass) {
    Map<String, Object> consumerConfig = configProvider.getConsumerConfig(topicName);
    consumerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    return new FailFastDefaultKafkaConsumerFactory<>(
        topicName,
        consumerConfig,
        new StringDeserializer(),
        deserializerSupplier.supplyFor(messageClass),
        bootstrapServersSupplier
    );
  }

  public ConfigProvider getConfigProvider() {
    return configProvider;
  }

  ClusterMetaInfoProvider getClusterMetaInfoProvider() {
    return clusterMetaInfoProvider;
  }

  TopicPartitionsMonitoring getTopicPartitionsMonitoring() {
    return topicPartitionsMonitoring;
  }
}
