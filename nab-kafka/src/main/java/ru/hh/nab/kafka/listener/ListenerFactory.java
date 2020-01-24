package ru.hh.nab.kafka.listener;

public interface ListenerFactory {

  <T> Listener listenTopic(String topicName,
                           String operationName,
                           Class<T> messageClass,
                           ListenStrategy<T> messageListener);

}
