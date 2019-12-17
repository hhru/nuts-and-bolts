package ru.hh.nab.kafka.consumer;

import java.util.List;

public interface ListenStrategy<T> {

  void onMessagesBatch(List<T> messages, Ack ack);

}
