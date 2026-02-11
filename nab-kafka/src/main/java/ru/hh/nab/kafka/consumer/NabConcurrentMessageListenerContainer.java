package ru.hh.nab.kafka.consumer;

import java.util.concurrent.ExecutorService;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

public class NabConcurrentMessageListenerContainer<K, V> extends ConcurrentMessageListenerContainer<K, V> {

  private final ExecutorService messageProcessingExecutor;

  public NabConcurrentMessageListenerContainer(
      ConsumerFactory<? super K, ? super V> consumerFactory,
      ContainerProperties containerProperties,
      ExecutorService messageProcessingExecutor
  ) {
    super(consumerFactory, containerProperties);
    this.messageProcessingExecutor = messageProcessingExecutor;
  }

  @Override
  protected void doStart() {
    if (!isRunning()) {
      super.doStart();
      ConsumerShutdownCoordinator.onConsumerStarted(getBeanName());
    }
  }

  @Override
  protected void doStop(Runnable callback, boolean normal) {
    if (isRunning()) {
      String consumer = getBeanName();
      ConsumerShutdownCoordinator.onConsumerShutdownStarted(consumer);
      super.doStop(
          () -> {
            callback.run();
            ConsumerShutdownCoordinator.onConsumerShutdownCompleted(consumer);
          },
          normal
      );
      messageProcessingExecutor.shutdownNow();
    }
  }
}
