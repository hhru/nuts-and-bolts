package ru.hh.nab.kafka.consumer.retry;

public record RetryTopics(String retrySendTopic, String retryReceiveTopic) {
  private static final String DEFAULT_RETRY_SEND_TOPIC = "%s_%s_retry_send";
  private static final String DEFAULT_RETRY_RECEIVE_TOPIC = "%s_%s_retry_receive";
  public static RetryTopics DEFAULT_SINGLE_TOPIC = new RetryTopics(DEFAULT_RETRY_RECEIVE_TOPIC, DEFAULT_RETRY_RECEIVE_TOPIC);
  public static RetryTopics DEFAULT_PAIR_OF_TOPICS = new RetryTopics(DEFAULT_RETRY_SEND_TOPIC, DEFAULT_RETRY_RECEIVE_TOPIC);

  public static RetryTopics single(String retryTopic) {
    return new RetryTopics(retryTopic, retryTopic);
  }

  public static RetryTopics defaultSingleTopic(String mainTopic, String operationName) {
    String defaultSingleTopic = DEFAULT_RETRY_RECEIVE_TOPIC.formatted(mainTopic, operationName);
    return new RetryTopics(defaultSingleTopic, defaultSingleTopic);
  }

  public static RetryTopics defaultPairOfTopics(String mainTopic, String operationName) {
    return new RetryTopics(
        DEFAULT_RETRY_SEND_TOPIC.formatted(mainTopic, operationName),
        DEFAULT_RETRY_RECEIVE_TOPIC.formatted(mainTopic, operationName)
    );
  }

  public boolean isSingleTopic() {
    return retrySendTopic.equals(retryReceiveTopic);
  }
}
