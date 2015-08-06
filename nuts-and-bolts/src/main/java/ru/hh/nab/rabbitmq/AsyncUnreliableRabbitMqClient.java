package ru.hh.nab.rabbitmq;

import javax.inject.Inject;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncUnreliableRabbitMqClient extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(AsyncUnreliableRabbitMqClient.class);

  private static final int BUF_SIZE = 1000;

  private final CircularFifoQueue<ChannelAction> queue = new CircularFifoQueue<>(BUF_SIZE);
  private final UnreliableRabbitMqClient mqClient;
  private final Object mutex = new Object();

  @Inject
  public AsyncUnreliableRabbitMqClient(UnreliableRabbitMqClient mqClient) {
    this.mqClient = mqClient;
  }

  private boolean isQueueEmpty() {
    synchronized (queue) {
      return queue.isEmpty();
    }
  }

  private ChannelAction queueRemove() {
    synchronized (queue) {
      return queue.remove();
    }
  }

  private void queueAdd(final ChannelAction action) {
    synchronized (queue) {
      if (queue.isFull()) {
        LOG.warn("Buffer overflow");
      }
      queue.add(action);
    }
  }

  @Override
  public void run() {
    while (!isInterrupted()) {
      synchronized (mutex) {
        while (isQueueEmpty()) {
          try {
            mutex.wait();
          } catch (InterruptedException e) {
            interrupt();
            return;
          }
        }
      }
      try {
        mqClient.maybePerform(queueRemove());
      } catch (Exception e) {
        LOG.error("Can't perform action", e);
      }
    }
  }

  public void maybePerformAsync(final ChannelAction action) {
    synchronized (mutex) {
      queueAdd(action);
      mutex.notify();
    }
  }
}
