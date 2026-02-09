package ru.hh.nab.kafka.consumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

public class NabConcurrentMessageListenerContainer<K, V> extends ConcurrentMessageListenerContainer<K, V> {

  private final ExecutorService messageProcessingExecutor;
  private final Logger logger;

  public NabConcurrentMessageListenerContainer(
      ConsumerFactory<? super K, ? super V> consumerFactory,
      ContainerProperties containerProperties,
      ExecutorService messageProcessingExecutor,
      Logger logger
  ) {
    super(consumerFactory, containerProperties);
    this.messageProcessingExecutor = messageProcessingExecutor;
    this.logger = logger;
  }

  @Override
  protected void doStop(Runnable callback, boolean normal) {
    if (isRunning()) {
      long shutdownTimeout = getContainerProperties().getShutdownTimeout();
      String consumer = getBeanName();
      logger.info("Consumer shutdown started: consumer={}, shutdownTimeout={}ms", consumer, shutdownTimeout);

      final CountDownLatch latch = new CountDownLatch(1);
      long shutdownStart = System.currentTimeMillis();
      super.doStop(
          () -> {
            callback.run();
            latch.countDown();
          },
          normal
      );
      messageProcessingExecutor.shutdownNow();
      try {
        if (latch.await(shutdownTimeout, TimeUnit.MILLISECONDS)) {
          logger.info(
              "Consumer shutdown took {}ms: consumer={}, shutdownTimeout={}ms",
              System.currentTimeMillis() - shutdownStart,
              consumer,
              shutdownTimeout
          );
        } else {
          logger.warn("Consumer shutdown timeout exceeded: consumer={}, shutdownTimeout={}ms", consumer, shutdownTimeout);
        }
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
