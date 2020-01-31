package ru.hh.nab.kafka.consumer;

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
import ru.hh.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.kafka.monitoring.MonitoringConsumeStrategy;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.metrics.StatsDSender;
import java.util.Collection;
import java.util.Map;

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

    var container = getSpringMessageListenerContainer(consumerFactory, containerProperties);
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
    return (data, acknowledgment) -> consumeStrategy.onMessagesBatch(data, acknowledgment::acknowledge);
  }

  private <T> ConcurrentMessageListenerContainer<String, T> getSpringMessageListenerContainer(ConsumerFactory<String, T> consumerFactory,
                                                                                              ContainerProperties containerProperties) {

    var container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    container.setBatchErrorHandler(new SeekToCurrentBatchErrorHandler());

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
