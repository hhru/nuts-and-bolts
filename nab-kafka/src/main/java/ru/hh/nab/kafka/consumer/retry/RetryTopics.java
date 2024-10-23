package ru.hh.nab.kafka.consumer.retry;

import ru.hh.nab.kafka.consumer.ConsumerBuilder;
import ru.hh.nab.kafka.consumer.ConsumerMetadata;

/**
 * Holds a pair of topics used to store messages scheduled for retry.
 * Messages are sent to <b>retrySendTopic</b> and later received from <b>retryReceiveTopic</b>.
 * In single-topic scheme this is the same topic.
 * You may use predefined {@link #DEFAULT_SINGLE_TOPIC} and {@link #DEFAULT_PAIR_OF_TOPICS} constants
 * that will be converted to conventional topic (pair of topics) names by {@link ConsumerBuilder}
 *
 * @see #defaultSingleTopic(ConsumerMetadata)
 * @see #defaultPairOfTopics(ConsumerMetadata)
 * */
public record RetryTopics(String retrySendTopic, String retryReceiveTopic) {
  private static final String DEFAULT_RETRY_SEND_TOPIC = "%s_%s_%s_retry_send";
  private static final String DEFAULT_RETRY_RECEIVE_TOPIC = "%s_%s_%s_retry_receive";
  public static RetryTopics DEFAULT_SINGLE_TOPIC = new RetryTopics(DEFAULT_RETRY_RECEIVE_TOPIC, DEFAULT_RETRY_RECEIVE_TOPIC);
  public static RetryTopics DEFAULT_PAIR_OF_TOPICS = new RetryTopics(DEFAULT_RETRY_SEND_TOPIC, DEFAULT_RETRY_RECEIVE_TOPIC);

  /**
   * Creates conventional name based on main topic name, service name and operation name:
   * topicName_serviceName_operationName<b>_retry_send</b>
   * */
  public static String defaultRetrySendTopic(ConsumerMetadata consumerMetadata) {
    return DEFAULT_RETRY_SEND_TOPIC.formatted(consumerMetadata.getTopic(), consumerMetadata.getServiceName(), consumerMetadata.getOperation());
  }

  /**
   * Creates conventional name based on main topic name, service name and operation name:
   * topicName_serviceName_operationName<b>_retry_receive</b>
   * */
  public static String defaultRetryReceiveTopic(ConsumerMetadata consumerMetadata) {
    return DEFAULT_RETRY_RECEIVE_TOPIC.formatted(consumerMetadata.getTopic(), consumerMetadata.getServiceName(), consumerMetadata.getOperation());
  }

  /**
   * Use single retry topic with custom name
   * */
  public static RetryTopics single(String retryTopic) {
    return new RetryTopics(retryTopic, retryTopic);
  }

  /**
   * Use single retry topic with conventional name
   * 
   * @see #defaultRetryReceiveTopic(ConsumerMetadata)
   * */
  public static RetryTopics defaultSingleTopic(ConsumerMetadata consumerMetadata) {
    return single(defaultRetryReceiveTopic(consumerMetadata));
  }

  /**
   * Use a pair of topics with conventional names
   * 
   * @see #defaultRetrySendTopic(ConsumerMetadata)
   * @see #defaultRetryReceiveTopic(ConsumerMetadata)
   * */
  public static RetryTopics defaultPairOfTopics(ConsumerMetadata consumerMetadata) {
    return new RetryTopics(defaultRetrySendTopic(consumerMetadata), defaultRetryReceiveTopic(consumerMetadata));
  }

  public boolean isSingleTopic() {
    return retrySendTopic.equals(retryReceiveTopic);
  }
}
