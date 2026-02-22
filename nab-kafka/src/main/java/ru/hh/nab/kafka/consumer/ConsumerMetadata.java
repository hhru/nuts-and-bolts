package ru.hh.nab.kafka.consumer;

import static java.util.Objects.requireNonNull;
import java.util.StringJoiner;

public class ConsumerMetadata {

  private final String consumerName;
  private final String topic;
  private final String operation;

  public ConsumerMetadata(String consumerName, String topic, String operation) {
    this.consumerName = requireNonNull(consumerName, "consumerName is required");
    this.topic = requireNonNull(topic, "topic is required");
    this.operation = operation != null ? operation : "";
  }

  public String getConsumerName() {
    return consumerName;
  }

  public String getTopic() {
    return topic;
  }

  public String getOperation() {
    return operation;
  }

  public String getConsumerGroupId() {
    return new StringJoiner("__")
        .add(consumerName)
        .add(topic)
        .add(operation)
        .toString();
  }

}
