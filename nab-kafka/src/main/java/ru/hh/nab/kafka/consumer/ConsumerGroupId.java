package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.StringJoiner;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;

public class ConsumerGroupId {

  private final String serviceName;
  private final String topic;
  private final String operation;

  private final List<Tag> tags;

  public ConsumerGroupId(String serviceName, String topic, String operation) {
    this.serviceName = serviceName;
    this.topic = topic;
    this.operation = operation;
    this.tags = List.of(
        new Tag(APP_TAG_NAME, serviceName),
        new Tag("topic", topic),
        new Tag("operation", operation)
    );
  }

  public String getTopic() {
    return topic;
  }

  public String getOperation() {
    return operation;
  }

  public List<Tag> toMetricTags() {
    return tags;
  }

  @Override
  public String toString() {
    return new StringJoiner("__")
        .add(serviceName)
        .add(topic)
        .add(operation)
        .toString();
  }
}
