package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.CommonClientConfigs;
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
import java.util.Map;
import java.util.StringJoiner;

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

    ContainerProperties containerProperties = getListenerContainerProperties(
        topicName,
        operationName,
        adaptToSpring(monitor(listenStrategy))
    );

    var container = getMessageListenerContainer(consumerFactory, containerProperties);
    container.start();

    return container::stop;
  }

  private <T> ListenStrategy<T> monitor(ListenStrategy<T> listenStrategy) {
    return new MonitoringListenStrategy<>(statsDSender, listenStrategy);
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

  private ContainerProperties getListenerContainerProperties(String topic, String operationName, GenericMessageListener<?> messageListener) {
    var containerProperties = new ContainerProperties(topic);
    containerProperties.setGroupId(getGroupId(topic, operationName));
    containerProperties.setAckOnError(false);
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(messageListener);
    return containerProperties;
  }

  private String getGroupId(String topic, String operationName) {
    return new StringJoiner("__")
        .add(configProvider.getServiceName())
        .add(topic)
        .add(operationName)
        .toString();
  }

}
