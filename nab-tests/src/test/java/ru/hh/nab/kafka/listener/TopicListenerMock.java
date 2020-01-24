package ru.hh.nab.kafka.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TopicListenerMock<T> implements ListenStrategy<T> {

  private List<T> receivedBatch = new ArrayList<>();

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack ack) {
    receivedBatch = messages.stream()
        .map(ConsumerRecord::value)
        .collect(Collectors.toList());

    ack.acknowledge();
  }

  void assertMessagesEquals(List<T> expectedMessages) {
    assertEquals(expectedMessages, receivedBatch);
  }
}
