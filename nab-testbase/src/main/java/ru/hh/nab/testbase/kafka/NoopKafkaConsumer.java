package ru.hh.nab.testbase.kafka;

import java.util.Collection;
import java.util.List;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import ru.hh.nab.kafka.consumer.KafkaConsumer;

public class NoopKafkaConsumer<T> extends KafkaConsumer<T> {

  public NoopKafkaConsumer() {
    super(null, null, null, null, null, null, null, null);
  }

  @Override
  public void start() {
    // do nothing
  }

  @Override
  public void stop(Runnable callback) {
    // do nothing
  }

  @Override
  public void stop() {
    // do nothing
  }

  @Override
  public Collection<TopicPartition> getAssignedPartitions() {
    return List.of();
  }

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer) {
    // do nothing
  }

  @Override
  protected void createNewSpringContainer() {
    // do nothing
  }

  @Override
  public void rewindToLastAckedOffset(Consumer<?, ?> consumer) {
    // do nothing
  }
}
