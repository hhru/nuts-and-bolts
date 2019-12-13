package ru.hh.nab.kafka.consumer;

public interface ListenerFactory {

  <T> Listener listenTopic(String topicName,
                           String operationName,
                           Class<T> messageClass,
                           ListenStrategy<T> messageListener);

}
