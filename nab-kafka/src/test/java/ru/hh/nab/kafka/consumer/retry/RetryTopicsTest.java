package ru.hh.nab.kafka.consumer.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.hh.nab.kafka.consumer.ConsumerMetadata;

class RetryTopicsTest {

  static final ConsumerMetadata CONSUMER_METADATA = new ConsumerMetadata(
      "service-with-retries",
      "mainTopic",
      "FizzBuzzProcessing"
  );

  @Test
  void defaultRetrySendTopic() {
    assertEquals("maintopic_service_with_retries_fizzbuzzprocessing_retry_send", RetryTopics.defaultRetrySendTopic(CONSUMER_METADATA));
  }

  @Test
  void defaultRetryReceiveTopic() {
    assertEquals("maintopic_service_with_retries_fizzbuzzprocessing_retry_receive", RetryTopics.defaultRetryReceiveTopic(CONSUMER_METADATA));
  }
}
