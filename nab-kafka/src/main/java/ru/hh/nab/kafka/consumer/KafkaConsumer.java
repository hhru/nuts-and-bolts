package ru.hh.nab.kafka.consumer;

import org.apache.kafka.common.TopicPartition;
import java.util.Collection;

public interface KafkaConsumer {

  void stopConsumer();

  Collection<TopicPartition> getAssignedPartitions();

}
