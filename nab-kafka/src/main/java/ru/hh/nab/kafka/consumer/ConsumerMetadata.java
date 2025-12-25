package ru.hh.nab.kafka.consumer;

import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.StringJoiner;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;

public class ConsumerMetadata {

  private final String serviceName;
  private final String topic;
  private final String operation;

  private final List<Tag> tags;

  public ConsumerMetadata(String serviceName, String topic, String operation) {
    this.serviceName = requireNonNull(serviceName, "serviceName is required");
    this.topic = requireNonNull(topic, "topic is required");
    this.operation = operation != null ? operation : "";
    this.tags = List.of(
        new Tag(APP_TAG_NAME, serviceName),
        new Tag("topic", topic),
        new Tag("operation", operation)
    );
  }

  public String getServiceName() {
    return serviceName;
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

  public String getConsumerGroupId() {
    return new StringJoiner("__")
        .add(serviceName)
        .add(topic)
        .add(operation)
        .toString();
  }

}
