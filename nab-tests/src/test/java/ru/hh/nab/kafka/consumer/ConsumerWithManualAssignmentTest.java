package ru.hh.nab.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.producer.ProducerRecord;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.hasItems;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import ru.hh.nab.kafka.KafkaTestConfig;

public class ConsumerWithManualAssignmentTest extends KafkaConsumerTestbase {
  private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

  private List<KafkaConsumer<?>> startedConsumers = new ArrayList<>();

  @AfterEach()
  void tearDown() {
    startedConsumers.forEach(KafkaConsumer::stop);
    startedConsumers = new ArrayList<>();
  }

  @Test
  public void testConsumeAllMessagesFromTheBeginning() {
    List<String> processedMessages = new ArrayList<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer);

    List<String> secondMessagesBatch = putMessagesIntoKafka(35);
    waitUntil(() -> assertEquals(40 + 35, processedMessages.size()));
    assertThat(processedMessages, hasItems(firstMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testConsumeAllMessagesFromEnd() throws InterruptedException {
    List<String> processedMessages = new ArrayList<>();
    putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.LATEST)
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer);
    Thread.sleep(500); // Консумеру нужно какое-то время, чтобы стартовать. Не нашел простого способа подписаться на успешный старт.

    List<String> secondMessagesBatch = putMessagesIntoKafka(35);
    waitUntil(() -> assertEquals(35, processedMessages.size()));
    assertThat(processedMessages, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testParallelProcessingByTwoConsumers() throws InterruptedException {
    Set<String> processedMessages1 = new LinkedHashSet<>();
    Set<String> processedMessages2 = new LinkedHashSet<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer1 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages1.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer1);

    KafkaConsumer<String> consumer2 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.LATEST)
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages2.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer2);
    Thread.sleep(500); // Консумеру нужно какое-то время, чтобы стартовать. Не нашел простого способа подписаться на успешный старт.

    List<String> secondMessagesBatch = putMessagesIntoKafka(35);
    waitUntil(() -> assertEquals(40 + 35, processedMessages1.size()));
    waitUntil(() -> assertEquals(35, processedMessages2.size()));

    assertThat(processedMessages1, hasItems(firstMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages1, hasItems(secondMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages2, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testHandleErrorBeforeCommit() {
    List<String> processedMessages = new ArrayList<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer);

    List<String> secondMessagesBatch = putMessagesIntoKafka(35);
    waitUntil(() -> assertEquals(40 + 35, processedMessages.size()));
    assertThat(processedMessages, hasItems(firstMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testDoSeekAfterEachMessage() {
    List<String> processedMessages = new ArrayList<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) ->
            messages.forEach(m -> {
              processedMessages.add(m.value());
              ack.seek(m);
            })
        )
        .start();
    startedConsumers.add(consumer);

    List<String> secondMessagesBatch = putMessagesIntoKafka(35);
    waitUntil(() -> assertEquals(40 + 35, processedMessages.size()));
    assertThat(processedMessages, hasItems(firstMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testDoAcknowledgeAfterEachMessage() {
    List<String> processedMessages = new ArrayList<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) ->
            messages.forEach(m -> {
              processedMessages.add(m.value());
              ack.acknowledge(m);
            }))
        .start();
    startedConsumers.add(consumer);

    List<String> secondMessagesBatch = putMessagesIntoKafka(35);
    waitUntil(() -> assertEquals(40 + 35, processedMessages.size()));
    assertThat(processedMessages, hasItems(firstMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testDoAcknowledgeOnMessagesBatch() {
    List<String> processedMessages = new ArrayList<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(77);

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages.add(m.value()));
          ack.acknowledge(messages);
        })
        .start();
    startedConsumers.add(consumer);

    List<String> secondMessagesBatch = putMessagesIntoKafka(30);
    waitUntil(() -> assertEquals(77 + 30, processedMessages.size()));
    assertThat(processedMessages, hasItems(firstMessagesBatch.toArray(String[]::new)));
    assertThat(processedMessages, hasItems(secondMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testReprocessingMessagesAfterError() throws InterruptedException {
    List<String> processedMessages = new ArrayList<>();
    List<String> firstMessagesBatch = putMessagesIntoKafka(100);

    CountDownLatch errorOccuredLatch = new CountDownLatch(1);
    int messagesToProcessBeforeError = 10;

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy((messages, ack) -> {
          for (int i = 0; i < messages.size(); i++) {
            processedMessages.add(messages.get(i).value());
            if (i == (messagesToProcessBeforeError - 1) && errorOccuredLatch.getCount() == 1) {
              errorOccuredLatch.countDown();
              throw new RuntimeException("Error on processing");
            }
          }
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer);
    assertTrue(errorOccuredLatch.await(3, TimeUnit.SECONDS));

    waitUntil(() -> assertEquals(100 + messagesToProcessBeforeError, processedMessages.size()));
    assertThat(processedMessages, hasItems(firstMessagesBatch.toArray(String[]::new)));
  }

  @Test
  public void testAssignNewPartitionsForTopicProcessedFromBeginning() throws InterruptedException, ExecutionException, JsonProcessingException {
    List<String> processedMessages1 = new ArrayList<>();
    List<String> processedMessages2 = new ArrayList<>();
    putMessagesIntoKafka(40);

    KafkaConsumer<String> consumer1 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST, Duration.ofSeconds(1))
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages1.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer1);

    KafkaConsumer<String> consumer2 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST, Duration.ofSeconds(1))
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> {
            processedMessages2.add(m.value());
            ack.seek(m);
          });
        })
        .start();
    startedConsumers.add(consumer2);
    assertEquals(5, consumer1.getAssignedPartitions().size());
    assertEquals(5, consumer2.getAssignedPartitions().size());

    addPartitions(topicName, 7);
    for (int i = 0; i < 7; i++) {
      putMessagesIntoKafka(10, i);
    }

    waitUntil(() -> assertEquals(40 + (10 * 7), processedMessages1.size()));
    waitUntil(() -> assertEquals(40 + (10 * 7), processedMessages2.size()));
    assertEquals(7, consumer1.getAssignedPartitions().size());
    assertEquals(7, consumer2.getAssignedPartitions().size());

    addPartitions(topicName, 9);
    for (int i = 0; i < 9; i++) {
      putMessagesIntoKafka(10, i);
    }

    waitUntil(() -> assertEquals(40 + (10 * 7) + (10 * 9), processedMessages1.size()));
    waitUntil(() -> assertEquals(40 + (10 * 7) + (10 * 9), processedMessages2.size()));
    assertEquals(9, consumer1.getAssignedPartitions().size());
    assertEquals(9, consumer2.getAssignedPartitions().size());
  }

  @Test
  public void testAssignNewPartitionsForTopicProcessedFromEnd() throws InterruptedException, ExecutionException, JsonProcessingException {
    List<String> processedMessages1 = new ArrayList<>();
    List<String> processedMessages2 = new ArrayList<>();

    KafkaConsumer<String> consumer1 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.LATEST, Duration.ofSeconds(1))
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages1.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer1);

    KafkaConsumer<String> consumer2 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.LATEST, Duration.ofSeconds(1))
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> {
            processedMessages2.add(m.value());
            ack.seek(m);
          });
        })
        .start();
    startedConsumers.add(consumer2);
    Thread.sleep(1000);

    putMessagesIntoKafka(40);

    addPartitions(topicName, 7);
    waitUntil(() -> assertEquals(7, consumer1.getAssignedPartitions().size()));
    waitUntil(() -> assertEquals(7, consumer2.getAssignedPartitions().size()));

    for (int i = 0; i < 7; i++) {
      putMessagesIntoKafka(10, i);
    }

    waitUntil(() -> assertEquals(40 + (10 * 7), processedMessages1.size()));
    waitUntil(() -> assertEquals(40 + (10 * 7), processedMessages2.size()));

    addPartitions(topicName, 9);
    waitUntil(() -> assertEquals(9, consumer1.getAssignedPartitions().size()));
    waitUntil(() -> assertEquals(9, consumer2.getAssignedPartitions().size()));
    for (int i = 0; i < 9; i++) {
      putMessagesIntoKafka(10, i);
    }

    waitUntil(() -> assertEquals(40 + (10 * 7) + (10 * 9), processedMessages1.size()));
    waitUntil(() -> assertEquals(40 + (10 * 7) + (10 * 9), processedMessages2.size()));
  }

  @Test
  public void testAddNewPartitionsDuringProcessing() throws InterruptedException, ExecutionException, JsonProcessingException {
    List<String> processedMessages1 = new ArrayList<>();

    KafkaConsumer<String> consumer1 = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("read_messages")
        .withAllPartitionsAssigned(SeekPosition.EARLIEST, Duration.ofMillis(500))
        .withConsumeStrategy((messages, ack) -> {
          messages.forEach(m -> processedMessages1.add(m.value()));
          ack.acknowledge();
        })
        .start();
    startedConsumers.add(consumer1);

    Executors.newSingleThreadExecutor().submit(() -> {
      putMessagesIntoKafka(500);
    });
    addPartitions(topicName, 6);
    assertThat(processedMessages1.size(), Matchers.lessThan(500));

    for (int i = 0; i < 6; i++) {
      putMessagesIntoKafka(10, i);
    }
    waitUntil(() -> assertEquals(500 + (10 * 6), processedMessages1.size()));
  }

  private List<String> putMessagesIntoKafka(int count) {
    List<String> messages = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String body = "body" + ID_SEQUENCE.incrementAndGet();
      kafkaTestUtils.sendMessage(topicName, body);
      messages.add(body);
    }
    return messages;
  }

  private List<String> putMessagesIntoKafka(int count, int partition) throws JsonProcessingException {
    List<String> messages = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String body = "body" + ID_SEQUENCE.incrementAndGet();
      kafkaTestUtils.sendMessage(new ProducerRecord<>(topicName, partition, null, KafkaTestConfig.OBJECT_MAPPER.writeValueAsBytes(body)));
      messages.add(body);
    }
    return messages;

  }

  protected void waitUntil(Runnable assertion) {
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(assertion::run);
  }

}
