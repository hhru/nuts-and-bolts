package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TopicConsumerMock<T> implements ConsumeStrategy<T> {

  private List<T> receivedBatch = new ArrayList<>();

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) {
    receivedBatch = messages.stream()
        .map(ConsumerRecord::value)
        .collect(Collectors.toList());

    ack.acknowledge();
  }

  void assertMessagesEquals(List<T> expectedMessages) {
    assertEquals(expectedMessages, receivedBatch);
  }
}
