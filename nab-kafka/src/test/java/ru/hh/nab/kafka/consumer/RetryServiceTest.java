package ru.hh.nab.kafka.consumer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getNextRetryTime;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.setNextRetryTime;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {
  static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
  static final Instant CREATION_TIME = NOW.minusSeconds(100);
  static final String TOPIC = "topic";
  static final String MESSAGE = "message";
  static final String KEY = "key";
  static long offset = 0;
  @Mock
  KafkaProducer kafkaProducer;
  @Captor
  ArgumentCaptor<ProducerRecord<String, String>> producerRecordCaptor;

  @Test
  void firstRetryAddsHeaders() {
    RetryService<String> retryService = createRetryService(RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(10))));
    MessageProcessingHistory processingHistory = MessageProcessingHistory.initial(CREATION_TIME, NOW);

    retryService.retry(message(), null);

    verify(kafkaProducer, only()).sendMessage(producerRecordCaptor.capture(), any());
    ProducerRecord<String, String> retryMessage = producerRecordCaptor.getValue();
    assertEquals(TOPIC + "_retry_send", retryMessage.topic());
    assertEquals(KEY, retryMessage.key());
    assertEquals(MESSAGE, retryMessage.value());
    assertEquals(processingHistory, getMessageProcessingHistory(retryMessage.headers()).get());
    assertEquals(NOW.plusSeconds(10), getNextRetryTime(retryMessage.headers()).get());
  }

  @Test
  void nextRetryReplacesHeaders() {
    RetryService<String> retryService = createRetryService(RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(10))));
    ConsumerRecord<String, String> message = message();
    MessageProcessingHistory oldProcessingHistory = new MessageProcessingHistory(CREATION_TIME.plusSeconds(1), 9, NOW.minusSeconds(90));
    setMessageProcessingHistory(message.headers(), oldProcessingHistory);
    setNextRetryTime(message.headers(), NOW);

    retryService.retry(message, null);

    verify(kafkaProducer, only()).sendMessage(producerRecordCaptor.capture(), any());
    ProducerRecord<String, String> retryMessage = producerRecordCaptor.getValue();
    assertEquals(TOPIC + "_retry_send", retryMessage.topic());
    assertEquals(KEY, retryMessage.key());
    assertEquals(MESSAGE, retryMessage.value());
    assertNotEquals(oldProcessingHistory,  getMessageProcessingHistory(retryMessage.headers()).get());
    assertEquals(oldProcessingHistory.withOneMoreFail(NOW), getMessageProcessingHistory(retryMessage.headers()).get());
    assertEquals(NOW.plusSeconds(10), getNextRetryTime(retryMessage.headers()).get());
  }

  @Test
  void testStopRetriesDueToPolicyLimits() {
    RetryService<String> retryService = createRetryService(
        RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(10)).withRetryLimit(1)));
    MessageProcessingHistory processingHistory = MessageProcessingHistory.initial(CREATION_TIME, NOW);
    ConsumerRecord<String, String> message = message();
    setMessageProcessingHistory(message.headers(), processingHistory);

    retryService.retry(message, null);

    verify(kafkaProducer, never()).sendMessage(producerRecordCaptor.capture(), any());
  }

  @Test
  void testStopRetriesForSpecificException() {
    RetryService<String> retryService = createRetryService(
        (message, throwable) -> throwable instanceof IllegalStateException ? RetryPolicy.never() : RetryPolicy.fixed(Duration.ofSeconds(10))
    );
    retryService.retry(message(), new IllegalStateException());
    verify(kafkaProducer, never()).sendMessage(producerRecordCaptor.capture(), any());
    retryService.retry(message(), null);
    verify(kafkaProducer, only()).sendMessage(producerRecordCaptor.capture(), any());
  }

  @Test
  void testStopRetriesForSpecificMessage() {
    RetryService<String> retryService = createRetryService(
        (message, throwable) -> message.value().equals("non-retriable") ? RetryPolicy.never() : RetryPolicy.fixed(Duration.ofSeconds(10))
    );
    retryService.retry(message("non-retriable"), null);
    verify(kafkaProducer, never()).sendMessage(producerRecordCaptor.capture(), any());
    retryService.retry(message(), null);
    verify(kafkaProducer, only()).sendMessage(producerRecordCaptor.capture(), any());
  }

  private RetryService<String> createRetryService(RetryPolicyResolver<String> retryPolicyResolver) {
    return new RetryService<>(
        kafkaProducer,
        TOPIC + "_retry_send",
        retryPolicyResolver,
        Clock.fixed(NOW, ZoneId.systemDefault())
    );
  }

  private static ConsumerRecord<String, String> message() {
    return message(MESSAGE);
  }

  private static ConsumerRecord<String, String> message(String message) {
    return new ConsumerRecord<>(
        TOPIC,
        0,
        offset++,
        CREATION_TIME.toEpochMilli(),
        TimestampType.CREATE_TIME,
        0,
        0,
        KEY,
        message,
        new RecordHeaders(),
        Optional.empty()
    );
  }
}