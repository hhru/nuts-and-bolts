package ru.hh.nab.kafka.consumer;

import java.util.Arrays;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.mockito.MockitoAnnotations;

/**
 * Test class for {@link ConsumeStrategy} interface.
 * Tests both atLeastOnceWithBatchAck and atLeastOnceWithBatchAckWithoutRetries strategies.
 */
class ConsumeStrategyTest {

  @Mock
  private MessageProcessor<String> messageProcessor;

  @Mock
  private Ack<String> ack;

  private List<ConsumerRecord<String, String>> records;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    records = Arrays.asList(
        new ConsumerRecord<>("topic", 0, 0, "key1", "value1"),
        new ConsumerRecord<>("topic", 0, 1, "key2", "value2")
    );
  }

  /**
   * Tests the happy path scenario for atLeastOnceWithBatchAck strategy.
   * Verifies that:
   * 1. All messages in the batch are processed successfully
   * 2. The batch is acknowledged after all messages are processed
   * 3. No retries are triggered
   */
  @Test
  void atLeastOnceWithBatchAckShouldProcessAllMessagesAndAcknowledge() throws InterruptedException {
    // Given
    ConsumeStrategy<String> strategy = ConsumeStrategy.atLeastOnceWithBatchAck(messageProcessor);

    // When
    strategy.onMessagesBatch(records, ack);

    // Then
    verify(messageProcessor).process("value1");
    verify(messageProcessor).process("value2");
    verify(ack).acknowledge();
    verifyNoMoreInteractions(ack);
  }

  /**
   * Tests the error handling scenario for atLeastOnceWithBatchAck strategy.
   * Verifies that:
   * 1. When a message processing fails, it triggers a retry via Ack.retry()
   * 2. The remaining messages in the batch are still processed
   * 3. The batch is acknowledged after all messages are either processed or retried
   */
  @Test
  void atLeastOnceWithBatchAckShouldRetryOnFailure() throws InterruptedException {
    // Given
    ConsumeStrategy<String> strategy = ConsumeStrategy.atLeastOnceWithBatchAck(messageProcessor);
    RuntimeException exception = new RuntimeException("Processing failed");
    doThrow(exception).when(messageProcessor).process("value1");

    // When
    strategy.onMessagesBatch(records, ack);

    // Then
    verify(messageProcessor).process("value1");
    verify(messageProcessor).process("value2");
    verify(ack).retry(records.get(0), exception);
    verify(ack).acknowledge();
    verifyNoMoreInteractions(ack);
  }

  /**
   * Tests the happy path scenario for atLeastOnceWithBatchAckWithoutRetries strategy.
   * Verifies that:
   * 1. Each message is processed sequentially
   * 2. Each message is explicitly acknowledged via seek() before moving to the next one
   * 3. The batch is acknowledged after all messages are processed
   */
  @Test
  void atLeastOnceWithBatchAckWithoutRetriesShouldProcessAndSeekEachMessage() throws InterruptedException {
    // Given
    ConsumeStrategy<String> strategy = ConsumeStrategy.atLeastOnceWithBatchAckWithoutRetries(messageProcessor);

    // When
    strategy.onMessagesBatch(records, ack);

    // Then
    verify(messageProcessor).process("value1");
    verify(messageProcessor).process("value2");
    verify(ack).seek(records.get(0));
    verify(ack).seek(records.get(1));
    verify(ack).acknowledge();
    verifyNoMoreInteractions(ack);
  }

  /**
   * Tests the error handling scenario for atLeastOnceWithBatchAckWithoutRetries strategy.
   * Verifies that:
   * 1. When a message processing fails, the exception is propagated immediately
   * 2. No further messages are processed after the failure
   * 3. No acknowledgments or seeks are performed after the failure
   */
  @Test
  void atLeastOnceWithBatchAckWithoutRetriesShouldPropagateException() throws InterruptedException {
    // Given
    ConsumeStrategy<String> strategy = ConsumeStrategy.atLeastOnceWithBatchAckWithoutRetries(messageProcessor);
    RuntimeException exception = new RuntimeException("Processing failed");
    doThrow(exception).when(messageProcessor).process("value1");

    // When/Then
    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> {
          strategy.onMessagesBatch(records, ack);
        }
    );

    verify(messageProcessor).process("value1");
    verifyNoMoreInteractions(messageProcessor);
    verifyNoMoreInteractions(ack);
  }
}
