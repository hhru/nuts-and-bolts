package ru.hh.nab.kafka.consumer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.kafka.util.SleepUtils;

public final class ConsumerShutdownCoordinator {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerShutdownCoordinator.class);

  private static final Set<String> startedConsumers = ConcurrentHashMap.newKeySet();
  private static final Map<String, Long> shutdownStartedConsumers = new ConcurrentHashMap<>();
  private static final Set<String> shutdownCompletedConsumers = ConcurrentHashMap.newKeySet();

  static void onConsumerStarted(String consumer) {
    startedConsumers.add(consumer);
  }

  static void onConsumerShutdownStarted(String consumer) {
    shutdownStartedConsumers.put(consumer, System.currentTimeMillis());
    logger.info("Consumer shutdown started: {}", consumer);
  }

  static void onConsumerShutdownCompleted(String consumer) {
    shutdownCompletedConsumers.add(consumer);
    Long shutdownStartedTimeMillis = shutdownStartedConsumers.get(consumer);
    if (shutdownStartedTimeMillis == null) {
      logger.info("Consumer shutdown completed: {}", consumer);
    } else {
      logger.info("Consumer shutdown completed (took {}ms): {}", System.currentTimeMillis() - shutdownStartedTimeMillis, consumer);
    }
  }

  public static void awaitTermination(long timeoutMs) {
    if (!startedConsumers.isEmpty()) {
      Set<String> runningConsumers = startedConsumers
          .stream()
          .filter(Predicate.not(shutdownStartedConsumers::containsKey))
          .collect(Collectors.toUnmodifiableSet());
      if (!runningConsumers.isEmpty()) {
        logger.error("Some consumers are still running, possibly you forgot to stop them: [{}]", String.join(", ", runningConsumers));
      }

      try {
        SleepUtils.stoppableSleep(timeoutMs, () -> shutdownCompletedConsumers.size() != shutdownStartedConsumers.size());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      Set<String> shutdownNotCompletedConsumers = shutdownStartedConsumers
          .keySet()
          .stream()
          .filter(Predicate.not(shutdownCompletedConsumers::contains))
          .collect(Collectors.toUnmodifiableSet());
      if (!shutdownNotCompletedConsumers.isEmpty()) {
        logger.warn(
            "Consumer global shutdown timeout {}ms exceeded, but some consumers didn't stop: [{}]",
            timeoutMs,
            String.join(", ", shutdownNotCompletedConsumers)
        );
      }
    }
  }

  private ConsumerShutdownCoordinator() {
  }
}
