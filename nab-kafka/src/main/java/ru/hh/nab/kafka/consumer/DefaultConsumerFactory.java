package ru.hh.nab.kafka.consumer;

import java.util.Properties;
import java.util.function.Supplier;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.backoff.ExponentialBackOff;
import ru.hh.nab.common.properties.PropertiesUtils;
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
  private final ClusterMetadataProvider clusterMetadataProvider;
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
    this.clusterMetadataProvider = new ClusterMetadataProvider(this);
    this.topicPartitionsMonitoring = new TopicPartitionsMonitoring(this.clusterMetadataProvider);
  }

  @Override
  public <T> ConsumerBuilder<T> builder(String topicName, Class<T> messageClass) {
    return new DefaultConsumerBuilder<>(this, topicName, messageClass)
        .withLogger(factoryLogger);
  }

  public <T> ConsumeStrategy<T> interceptConsumeStrategy(ConsumerMetadata consumerMetadata, ConsumeStrategy<T> consumeStrategy) {
    return new MonitoringConsumeStrategy<>(statsDSender, consumerMetadata, consumeStrategy);
  }


  <T> SeekToFirstNotAckedMessageErrorHandler<T> getCommonErrorHandler(
      String topicName,
      KafkaConsumer<T> kafkaConsumer,
      Logger logger
  ) {
    Properties settings = configProvider.getNabConsumerSettings(topicName);
    ExponentialBackOff backOff = new ExponentialBackOff(
        PropertiesUtils.getLong(settings, BACKOFF_INITIAL_INTERVAL_NAME, DEFAULT_BACKOFF_INITIAL_INTERVAL),
        PropertiesUtils.getDouble(settings, BACKOFF_MULTIPLIER_NAME, DEFAULT_BACKOFF_MULTIPLIER)
    );
    backOff.setMaxInterval(PropertiesUtils.getLong(settings, BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL));
    return new SeekToFirstNotAckedMessageErrorHandler<>(logger, backOff, kafkaConsumer, configProvider.getServiceName(), statsDSender);
  }

  <T> ConsumerFactory<String, T> getSpringConsumerFactory(String topicName, Class<T> messageClass) {
    return new FailFastDefaultKafkaConsumerFactory<>(
        topicName,
        configProvider.getConsumerConfig(topicName),
        new StringDeserializer(),
        deserializerSupplier.supplyFor(messageClass),
        bootstrapServersSupplier
    );
  }

  public ConfigProvider getConfigProvider() {
    return configProvider;
  }

  ClusterMetadataProvider getClusterMetadataProvider() {
    return clusterMetadataProvider;
  }

  TopicPartitionsMonitoring getTopicPartitionsMonitoring() {
    return topicPartitionsMonitoring;
  }

  StatsDSender getStatsDSender() {
    return statsDSender;
  }
}
