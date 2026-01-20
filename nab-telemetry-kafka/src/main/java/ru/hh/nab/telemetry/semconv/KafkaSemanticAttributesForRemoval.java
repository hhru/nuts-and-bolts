package ru.hh.nab.telemetry.semconv;

import io.opentelemetry.api.common.AttributeKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;

/**
 * TODO HH-278535
 * @deprecated attributes from this class will be removed and only need for smooth transition
 */
@Deprecated(forRemoval = true)
public class KafkaSemanticAttributesForRemoval {

  /**
   * @deprecated TODO HH-278535 this attribute will be removed without any replacement, don't use it in your code.
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> MESSAGING_DESTINATION_KIND = stringKey("messaging.destination.kind");

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use MessagingIncubatingAttributes.MESSAGING_CLIENT_ID instead
   * @see io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes#MESSAGING_CLIENT_ID
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> MESSAGING_KAFKA_CLIENT_ID = stringKey("messaging.kafka.client_id");

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use MessagingIncubatingAttributes.MESSAGING_CONSUMER_GROUP_NAME instead
   * @see io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes#MESSAGING_CONSUMER_GROUP_NAME
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> MESSAGING_KAFKA_CONSUMER_GROUP = MessagingIncubatingAttributes.MESSAGING_KAFKA_CONSUMER_GROUP;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use MessagingIncubatingAttributes.MESSAGING_OPERATION_TYPE instead
   * @see io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes#MESSAGING_OPERATION_TYPE
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> MESSAGING_OPERATION = MessagingIncubatingAttributes.MESSAGING_OPERATION;

  private KafkaSemanticAttributesForRemoval() {}
}
