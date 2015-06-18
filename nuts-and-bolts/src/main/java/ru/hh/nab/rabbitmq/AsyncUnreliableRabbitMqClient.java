package ru.hh.nab.rabbitmq;

import javax.inject.Inject;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncUnreliableRabbitMqClient extends Thread {
  private final Logger log = LoggerFactory.getLogger(AsyncUnreliableRabbitMqClient.class);

  private static final int BUF_SIZE = 1000;

  private final CircularFifoBuffer buffer = new CircularFifoBuffer(BUF_SIZE);
  private final UnreliableRabbitMqClient mqClient;
  private final Object mutex = new Object();

  @Inject
  public AsyncUnreliableRabbitMqClient(UnreliableRabbitMqClient mqClient) {
    this.mqClient = mqClient;
  }

  private boolean isBufferEmpty() {
    synchronized (buffer) {
      return buffer.isEmpty();
    }
  }

  private ChannelAction bufferRemove() {
    synchronized (buffer) {
      return (ChannelAction) buffer.remove();
    }
  }

  private void bufferAdd(ChannelAction action) {
    synchronized (buffer) {
      if (buffer.isFull()) {
        log.warn("Buffer overflow");
      }
      buffer.add(action);
    }
  }

  @Override
  public void run() {
    while (!isInterrupted()) {
      synchronized (mutex) {
        while (isBufferEmpty()) {
          try {
            mutex.wait();
          } catch (InterruptedException e) {
            interrupt();
            return;
          }
        }
      }
      try {
        mqClient.maybePerform(bufferRemove());
      } catch (Exception e) {
        log.error("Can't perform action", e);
      }
    }
  }

  public void maybePerformAsync(ChannelAction action) {
    synchronized (mutex) {
      bufferAdd(action);
      mutex.notify();
    }
  }
}
