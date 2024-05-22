package ru.hh.nab.kafka.consumer;

import org.springframework.kafka.support.TopicPartitionOffset;

public enum SeekPosition {

  BEGINNING(TopicPartitionOffset.SeekPosition.BEGINNING),

  END(TopicPartitionOffset.SeekPosition.END);

  private final TopicPartitionOffset.SeekPosition springKafkaSeekPosition;

  SeekPosition(TopicPartitionOffset.SeekPosition springKafkaSeekPosition) {
    this.springKafkaSeekPosition = springKafkaSeekPosition;
  }

  public TopicPartitionOffset.SeekPosition getSpringSeekPosition() {
    return springKafkaSeekPosition;
  }
}
