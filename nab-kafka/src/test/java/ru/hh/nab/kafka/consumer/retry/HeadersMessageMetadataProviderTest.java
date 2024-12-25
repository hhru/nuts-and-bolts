package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getNextRetryTime;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getRetryReceiveTopic;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setNextRetryTime;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setRetryReceiveTopic;

class HeadersMessageMetadataProviderTest {
  static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

  @Test
  void messageProcessingHistory() {
    Headers headers = new RecordHeaders();
    assertTrue(getMessageProcessingHistory(headers).isEmpty());
    MessageProcessingHistory processingHistory = new MessageProcessingHistory(NOW.minusSeconds(1), 17, NOW);
    setMessageProcessingHistory(headers, processingHistory);
    assertEquals(processingHistory, getMessageProcessingHistory(headers).get());
  }

  @Test
  void nextRetryTime() {
    Headers headers = new RecordHeaders();
    assertTrue(getNextRetryTime(headers).isEmpty());
    setNextRetryTime(headers, NOW);
    assertEquals(NOW, getNextRetryTime(headers).get());
  }

  @Test
  void retryReceiveTopic() {
    Headers headers = new RecordHeaders();
    assertTrue(getRetryReceiveTopic(headers).isEmpty());
    setRetryReceiveTopic(headers, new RetryTopics("sendTopic", "receiveTopic"));
    assertEquals("receiveTopic", getRetryReceiveTopic(headers).get());
  }
}
