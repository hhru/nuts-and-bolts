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
        long shutdownTimeout = getContainerProperties().getShutdownTimeout();
        if (latch.await(shutdownTimeout, TimeUnit.MILLISECONDS)) {
          logger.info(
              "Consumer shutdown took {}ms: consumer={}, shutdownTimeout={}ms",
              System.currentTimeMillis() - shutdownStart,
              getBeanName(),
              shutdownTimeout
          );
        } else {
          logger.warn("Consumer shutdown timeout exceeded: consumer={}, shutdownTimeout={}ms", getBeanName(), shutdownTimeout);
        }
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
