package ru.hh.nab.kafka.listener;

import org.apache.kafka.common.TopicPartition;
import java.util.Collection;

public interface Listener {

  void stopListen();

  Collection<TopicPartition> getAssignedPartitions();

}
