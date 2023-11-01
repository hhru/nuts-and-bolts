package ru.hh.nab.kafka.consumer;

@FunctionalInterface
public interface MessageProcessor<M> {
  void process(M message) throws InterruptedException;
}
