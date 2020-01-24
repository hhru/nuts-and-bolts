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
import ru.hh.nab.kafka.monitoring.MonitoringListenStrategy;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.metrics.StatsDSender;
import java.util.Collection;
import java.util.Map;

public class DefaultListenerFactory implements ListenerFactory {

  private final ConfigProvider configProvider;
  private final DeserializerSupplier deserializerSupplier;
  private final StatsDSender statsDSender;

  public DefaultListenerFactory(ConfigProvider configProvider,
                                DeserializerSupplier deserializerSupplier,
                                StatsDSender statsDSender) {
    this.configProvider = configProvider;
    this.deserializerSupplier = deserializerSupplier;
    this.statsDSender = statsDSender;
  }

  public <T> Listener listenTopic(String topicName,
                                  String operationName,
                                  Class<T> messageClass,
                                  ListenStrategy<T> listenStrategy) {

    ConsumerFactory<String, T> consumerFactory = getConsumerFactory(topicName, messageClass);

    ListenerGroupId listenerGroupId = new ListenerGroupId(configProvider.getServiceName(), topicName, operationName);
    ContainerProperties containerProperties = getListenerContainerProperties(
        listenerGroupId,
        adaptToSpring(monitor(listenerGroupId, listenStrategy))
    );

    var container = getMessageListenerContainer(consumerFactory, containerProperties);
    container.start();

    return new Listener() {
      @Override
      public void stopListen() {
        container.stop();
      }

      @Override
      public Collection<TopicPartition> getAssignedPartitions() {
        return container.getAssignedPartitions();
      }
    };
  }

  private <T> ListenStrategy<T> monitor(ListenerGroupId listenerGroupId, ListenStrategy<T> listenStrategy) {
    return new MonitoringListenStrategy<>(statsDSender, listenerGroupId, listenStrategy);
  }

  private <T> BatchAcknowledgingMessageListener<String, T> adaptToSpring(ListenStrategy<T> listenStrategy) {
    return (data, acknowledgment) -> listenStrategy.onMessagesBatch(data, acknowledgment::acknowledge);
  }

  private <T> ConcurrentMessageListenerContainer<String, T> getMessageListenerContainer(ConsumerFactory<String, T> consumerFactory,
                                                                                        ContainerProperties containerProperties) {

    var container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    container.setBatchErrorHandler(new SeekToCurrentBatchErrorHandler());

    return container;
  }

  private <T> ConsumerFactory<String, T> getConsumerFactory(String topicName, Class<T> messageClass) {
    Map<String, Object> consumerConfig = configProvider.getConsumerConfig(topicName);
    consumerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, KafkaStatsDReporter.class.getName());

    return new DefaultKafkaConsumerFactory<>(
        consumerConfig,
        new StringDeserializer(),
        deserializerSupplier.supplyFor(messageClass)
    );
  }

  private ContainerProperties getListenerContainerProperties(ListenerGroupId listenerGroupId,
                                                             GenericMessageListener<?> messageListener) {
    var containerProperties = new ContainerProperties(listenerGroupId.getTopic());
    containerProperties.setGroupId(listenerGroupId.toString());
    containerProperties.setAckOnError(false);
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    return containerProperties;
  }


}
