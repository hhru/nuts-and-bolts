package ru.hh.nab.kafka.consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConsumerRecoveryAfterFailTest extends KafkaConsumerTestBase {
  private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);
  private List<String> processedMessages;
  private KafkaConsumer<String> consumer;

  @BeforeEach
  public void setUp() {
    processedMessages = synchronizedList(new ArrayList<>());
  }

  @Test
  public void testNoGlobalAckPerformed() throws InterruptedException {
    putMessagesIntoKafka(117);

    startConsumer((messages, ack) -> messages.forEach(m -> processedMessages.add(m.value())));
    assertProcessedMessagesMoreThan(500);
  }

  @Test
  public void testSeek() throws InterruptedException {
    putMessagesIntoKafka(117);
    AtomicBoolean failed = new AtomicBoolean(false);
    startConsumer((messages, ack) -> {
      messages.forEach(m -> {
        processedMessages.add(m.value());
        if (processedMessages.size() <= 40) {
          ack.seek(m);
        }
        if (!failed.get() && processedMessages.size() == 45) {
          failed.set(true);
          throw new IllegalStateException("Processing failed");
        }
      });
      ack.acknowledge();
    });
    assertProcessedMessagesCount(117 + 5);

    consumeAllRemainingMessages();
    assertProcessedMessagesCount(117 + 5);
    assertUniqueProcessedMessagesCount(117);
  }

  @Test
  public void testSeekWithoutAck() throws InterruptedException {
    putMessagesIntoKafka(117);
    startConsumer((messages, ack) -> {
      messages.forEach(m -> {
        processedMessages.add(m.value());
        ack.seek(m);
      });
    });
    assertProcessedMessagesCount(117);

    consumeAllRemainingMessages();
    assertProcessedMessagesCount(117 * 2);
    assertUniqueProcessedMessagesCount(117);
  }

  @Test
  public void testAtMostOnceSemanticsWithBatchAck() throws InterruptedException {
    List<String> generatedMessages = putMessagesIntoKafka(117);
    Set<String> brokenMessages = Stream.of(40, 52, 64, 83).map(generatedMessages::get).collect(toSet());
    List<String> lostMessages = synchronizedList(new ArrayList<>());

    startConsumer((messages, ack) -> {
      ack.acknowledge();
      for (int i = 0; i < messages.size(); i++) {
        ConsumerRecord<String, String> m = messages.get(i);
        processedMessages.add(m.value());
        if (brokenMessages.contains(m.value())) {
          List<String> remainingBatch = messages.stream().map(ConsumerRecord::value).collect(toList()).subList(i + 1, messages.size());
          lostMessages.addAll(remainingBatch);
          throw new IllegalStateException("Processing failed");
        }
      }
    });
    waitUntil(() -> assertEquals(generatedMessages.size(), processedMessages.size() + lostMessages.size()));
    assertTrue(lostMessages.size() > 0);
    int processedMessagesAtFirstIteration = processedMessages.size();

    putMessagesIntoKafka(17);
    consumeAllRemainingMessages();
    assertProcessedMessagesCount(processedMessagesAtFirstIteration + 17);
    assertUniqueProcessedMessagesCount(processedMessagesAtFirstIteration + 17);

    lostMessages.forEach(message -> assertFalse(processedMessages.contains(message)));
  }

  @Test
  public void testAtMostOnceSemanticsWithEachMessageAck() throws InterruptedException {
    List<String> generatedMessages = putMessagesIntoKafka(117);
    Set<String> brokenMessages = Stream.of(40, 52).map(generatedMessages::get).collect(toSet());

    startConsumer((messages, ack) ->
        messages.forEach(m -> {
          ack.acknowledge(m);
          processedMessages.add(m.value());
          if (brokenMessages.contains(m.value())) {
            throw new IllegalStateException("Processing failed");
          }
        })
    );
    assertProcessedMessagesCount(117);

    putMessagesIntoKafka(17);
    consumeAllRemainingMessages();
    assertProcessedMessagesCount(117 + 17);
    assertUniqueProcessedMessagesCount(117 + 17);
  }

  @Test
  public void testAtLeastOnceSemanticsWithBatchAck() throws Exception {
    List<String> generatedMessages = putMessagesIntoKafka(117);
    Set<String> brokenMessages = Stream.of(40, 52).map(generatedMessages::get).collect(toSet());
    CountDownLatch fails = new CountDownLatch(10);

    Set<String> duplicatedMessages = synchronizedSet(new HashSet<>());
    startConsumer((messages, ack) -> {
      List<String> processedBatch = new ArrayList<>();
      messages.forEach(m -> {
        processedMessages.add(m.value());
        processedBatch.add(m.value());
        if (brokenMessages.contains(m.value())) {
          duplicatedMessages.addAll(processedBatch);
          fails.countDown();
          throw new IllegalStateException("Processing failed");
        }
      });
      ack.acknowledge();
    });
    assertTrue(fails.await(15, TimeUnit.SECONDS));
    stopConsumer();

    putMessagesIntoKafka(17);
    consumeAllRemainingMessages();
    waitUntil(() -> assertUniqueProcessedMessagesCount(117 + 17));
    Map<String, Long> messageProcessedCount = getProcessedMessagesCount();

    processedMessages
        .stream()
        .filter(Predicate.not(duplicatedMessages::contains))
        .forEach(m -> assertEquals(1L, messageProcessedCount.get(m).longValue()));
    duplicatedMessages.forEach(m -> assertNotEquals(1L, messageProcessedCount.get(m).longValue()));
  }

  @Test
  public void testAtLeastOnceSemanticsWithAckAfterEachMessage() throws Exception {
    List<String> generatedMessages = putMessagesIntoKafka(117);
    Set<String> brokenMessages = Stream.of(40, 52).map(generatedMessages::get).collect(toSet());
    CountDownLatch fails = new CountDownLatch(10);

    startConsumer((messages, ack) -> {
      messages.forEach(m -> {
        processedMessages.add(m.value());
        if (brokenMessages.contains(m.value())) {
          fails.countDown();
          throw new IllegalStateException("Processing failed");
        }
        ack.acknowledge(m);
      });
    });
    assertTrue(fails.await(15, TimeUnit.SECONDS));
    stopConsumer();

    putMessagesIntoKafka(17);
    consumeAllRemainingMessages();
    waitUntil(() -> assertUniqueProcessedMessagesCount(117 + 17));
    Map<String, Long> messageProcessedCount = getProcessedMessagesCount();

    processedMessages
        .stream()
        .filter(Predicate.not(brokenMessages::contains))
        .forEach(m -> assertEquals(1L, messageProcessedCount.get(m).longValue()));
  }

  @Test
  public void testAckLastMessageOfLastBatchPartition() throws Exception {
    putMessagesIntoKafka(150);

    AtomicBoolean atLeastOneBatchHasSeveralPartitions = new AtomicBoolean(false);
    var partitionToUnAckedMessagesCountMapReference = new AtomicReference<Map<Integer, Long>>(Map.of());
    startConsumer((messages, ack) -> {
      var lastMessage = messages.get(messages.size() - 1);
      int lastMessagePartition = lastMessage.partition();
      messages.forEach(m -> {
        if (m != lastMessage) {
          ack.seek(m);
          processedMessages.add(m.value());
        }
      });

      Map<Integer, Long> localPartitionToUnAckedMessagesCountMap = messages
          .stream()
          .filter(m -> m.partition() != lastMessagePartition)
          .collect(Collectors.groupingBy(ConsumerRecord::partition, Collectors.counting()));

      if (!localPartitionToUnAckedMessagesCountMap.isEmpty()) {
        atLeastOneBatchHasSeveralPartitions.set(true);
      }
      boolean successCAS = false;
      while (!successCAS) {
        Map<Integer, Long> previousMap = partitionToUnAckedMessagesCountMapReference.get();
        Map<Integer, Long> mutableMap = new HashMap<>(previousMap);
        mutableMap.remove(lastMessagePartition);
        localPartitionToUnAckedMessagesCountMap.forEach(
            (partition, countDelta) -> mutableMap.put(partition, mutableMap.getOrDefault(partition, 0L) + countDelta)
        );
        Map<Integer, Long> newMap = Collections.unmodifiableMap(mutableMap);
        successCAS = partitionToUnAckedMessagesCountMapReference.compareAndSet(previousMap, newMap);
      }

      ack.acknowledge(lastMessage);
      processedMessages.add(lastMessage.value());
    });
    assertProcessedMessagesCount(150);
    assertTrue(atLeastOneBatchHasSeveralPartitions.get());

    consumeAllRemainingMessages();
    assertProcessedMessagesCount(150 + (int) partitionToUnAckedMessagesCountMapReference.get().values().stream().mapToLong(v -> v).sum());
  }

  @Test
  public void testAckLastMessageOfEachPartition() throws Exception {
    putMessagesIntoKafka(150);

    startConsumer((messages, ack) -> {
      Collection<ConsumerRecord<String, String>> recordsWithMaxOffsetsInPartition = messages
          .stream()
          .collect(Collectors.toMap(
              ConsumerRecord::partition,
              Function.identity(),
              BinaryOperator.maxBy(Comparator.comparingLong(ConsumerRecord::offset))
          ))
          .values();

      messages.forEach(m -> processedMessages.add(m.value()));
      ack.acknowledge(recordsWithMaxOffsetsInPartition);
    });
    assertProcessedMessagesCount(150);

    consumeAllRemainingMessages();
    assertProcessedMessagesCount(150);
  }

  @Test
  public void testSeekWithAckInSameThreadWithDelay() throws InterruptedException {
    int originalNumberOfPublishedMessages = 117;
    int batchSize = 100;
    putMessagesIntoKafka(originalNumberOfPublishedMessages);
    final List<ConsumerRecord<String, String>> consumedRecords = Collections.synchronizedList(new ArrayList<>());
    startConsumer((messages, ack) -> {
      messages.forEach(m -> {
        consumedRecords.add(m);
        processedMessages.add(m.value());
        ack.seek(m);
      });
      if (consumedRecords.size() >= batchSize) {
        ack.commit(consumedRecords.stream().limit(batchSize).toList());
      }
    });
    assertProcessedMessagesCount(117);

    consumeAllRemainingMessages();
    //assert only 17 new messages consumed, because they were not committed
    assertProcessedMessagesCount(117 + 17);
  }

  private List<String> putMessagesIntoKafka(int count) {
    List<String> messages = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String body = "body" + ID_SEQUENCE.incrementAndGet();
      kafkaTestUtils.sendMessage(topicName, Integer.toString(i), body);
      messages.add(body);
    }
    return messages;
  }

  private void assertProcessedMessagesCount(int expectedCount) throws InterruptedException {
    waitUntil(() -> assertEquals(expectedCount, processedMessages.size()));
  }

  private void assertProcessedMessagesMoreThan(int expectedCount) throws InterruptedException {
    waitUntil(() -> assertTrue(expectedCount < processedMessages.size()));
  }

  private void assertUniqueProcessedMessagesCount(int expectedCount) {
    assertEquals(expectedCount, new CopyOnWriteArraySet<>(processedMessages).size());
  }

  protected void waitUntil(Runnable assertion) throws InterruptedException {
    await().atMost(15, TimeUnit.SECONDS).untilAsserted(assertion::run);
    stopConsumer();
  }

  private void consumeAllRemainingMessages() {
    startConsumer(
        (messages, ack) -> {
          messages.forEach(m -> processedMessages.add(m.value()));
          ack.acknowledge();
        }
    );
  }

  protected void startConsumer(ConsumeStrategy<String> consumerStrategy) {
    consumer = startMessagesConsumer(String.class, consumerStrategy);
  }

  private void stopConsumer() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    consumer.stop(latch::countDown);
    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  private Map<String, Long> getProcessedMessagesCount() {
    return processedMessages.stream().collect(groupingBy(Function.identity(), Collectors.counting()));
  }
}
