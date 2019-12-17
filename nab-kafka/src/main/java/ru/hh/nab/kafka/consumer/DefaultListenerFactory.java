package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.listener.SeekToCurrentBatchErrorHandler;
import ru.hh.nab.kafka.util.ConfigProvider;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class DefaultListenerFactory implements ListenerFactory {

  private final ConfigProvider configProvider;
  private final DeserializerSupplier deserializerSupplier;

  public DefaultListenerFactory(ConfigProvider configProvider, DeserializerSupplier deserializerSupplier) {
    this.configProvider = configProvider;
    this.deserializerSupplier = deserializerSupplier;
  }

  public <T> Listener listenTopic(String topicName,
                                  String operationName,
                                  Class<T> messageClass,
                                  ListenStrategy<T> messageListener) {

    ConsumerFactory<String, T> consumerFactory = getConsumerFactory(topicName, messageClass);

    ContainerProperties containerProperties = getListenerContainerProperties(topicName, operationName, adaptToSpring(messageListener));
    var container = getMessageListenerContainer(consumerFactory, containerProperties);
    container.start();

    return container::stop;
  }

  private <T> BatchAcknowledgingMessageListener<String, T> adaptToSpring(ListenStrategy<T> messageListener) {
    return (data, acknowledgment) -> messageListener.onMessagesBatch(
        data.stream().map(ConsumerRecord::value).collect(Collectors.toList()),
        acknowledgment::acknowledge
    );
  }

  private <T> ConcurrentMessageListenerContainer<String, T> getMessageListenerContainer(ConsumerFactory<String, T> consumerFactory,
                                                                                        ContainerProperties containerProperties) {

    var container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    container.setBatchErrorHandler(new SeekToCurrentBatchErrorHandler());

    return container;
  }

  private <T> ConsumerFactory<String, T> getConsumerFactory(String topicName, Class<T> messageClass) {
    Map<String, Object> consumerConfig = configProvider.getConsumerConfig(topicName);

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
