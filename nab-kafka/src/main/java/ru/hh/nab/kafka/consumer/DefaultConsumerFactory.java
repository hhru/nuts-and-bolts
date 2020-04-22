package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.listener.SeekToCurrentBatchErrorHandler;
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
  private final ConfigProvider configProvider;
  private final DeserializerSupplier deserializerSupplier;
  private final StatsDSender statsDSender;

  public DefaultConsumerFactory(ConfigProvider configProvider,
                                DeserializerSupplier deserializerSupplier,
                                StatsDSender statsDSender) {
    this.configProvider = configProvider;
    this.deserializerSupplier = deserializerSupplier;
    this.statsDSender = statsDSender;
  }

  public <T> KafkaConsumer subscribe(String topicName,
                                     String operationName,
                                     Class<T> messageClass,
                                     ConsumeStrategy<T> messageConsumer) {

    ConsumerFactory<String, T> consumerFactory = getSpringConsumerFactory(topicName, messageClass);

    ConsumerGroupId consumerGroupId = new ConsumerGroupId(configProvider.getServiceName(), topicName, operationName);
    ContainerProperties containerProperties = getSpringConsumerContainerProperties(
        consumerGroupId,
        adaptToSpring(monitor(consumerGroupId, messageConsumer))
    );

    var container = getSpringMessageListenerContainer(consumerFactory, containerProperties, topicName);
    container.start();

    return new KafkaConsumer() {
      @Override
      public void stopConsumer() {
        container.stop();
      }

      @Override
      public Collection<TopicPartition> getAssignedPartitions() {
        return container.getAssignedPartitions();
      }
    };
  }

  private <T> ConsumeStrategy<T> monitor(ConsumerGroupId consumerGroupId, ConsumeStrategy<T> consumeStrategy) {
    return new MonitoringConsumeStrategy<>(statsDSender, consumerGroupId, consumeStrategy);
  }

  private <T> BatchAcknowledgingMessageListener<String, T> adaptToSpring(ConsumeStrategy<T> consumeStrategy) {
    return (data, acknowledgment) -> consumeStrategy.onMessagesBatch(data, new Ack() {
      @Override
      public void acknowledge() {
        acknowledgment.acknowledge();
      }

      @Override
      public void nack(int index, long sleep) {
        acknowledgment.nack(index, sleep);
      }
    });
  }

  private <T> ConcurrentMessageListenerContainer<String, T> getSpringMessageListenerContainer(ConsumerFactory<String, T> consumerFactory,
                                                                                              ContainerProperties containerProperties,
                                                                                              String topicName) {
    var container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    SeekToCurrentBatchErrorHandler errorHandler = new SeekToCurrentBatchErrorHandler();
    FileSettings settings = configProvider.getNabConsumerSettings(topicName);
    ExponentialBackOff backOff = new ExponentialBackOff(
        settings.getLong(BACKOFF_INITIAL_INTERVAL_NAME, DEFAULT_BACKOFF_INITIAL_INTERVAL),
        settings.getDouble(BACKOFF_MULTIPLIER_NAME, DEFAULT_BACKOFF_MULTIPLIER)
    );
    backOff.setMaxInterval(settings.getLong(BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL));
    errorHandler.setBackOff(backOff);
    container.setBatchErrorHandler(errorHandler);

    return container;
  }

  private <T> ConsumerFactory<String, T> getSpringConsumerFactory(String topicName, Class<T> messageClass) {
    Map<String, Object> consumerConfig = configProvider.getConsumerConfig(topicName);
    consumerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    return new DefaultKafkaConsumerFactory<>(
        consumerConfig,
        new StringDeserializer(),
        deserializerSupplier.supplyFor(messageClass)
    );
  }

  private ContainerProperties getSpringConsumerContainerProperties(ConsumerGroupId consumerGroupId,
                                                                   GenericMessageListener<?> messageListener) {
    var containerProperties = new ContainerProperties(consumerGroupId.getTopic());
    containerProperties.setGroupId(consumerGroupId.toString());
    containerProperties.setAckOnError(false);
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    return containerProperties;
  }
}
