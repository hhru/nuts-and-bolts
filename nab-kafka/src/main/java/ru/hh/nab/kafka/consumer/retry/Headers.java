package ru.hh.nab.kafka.consumer.retry;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

public final class Headers {

  public static final String NUMBER_OF_FAILS = "numberOfFails";
  public static final String CREATION_TIME = "creationTime";
  public static final String LAST_FAIL_TIME = "lastFailTime";

  public static <T> Optional<Long> numberOfFails(ConsumerRecord<String, T> message) {
    return headerAsString(message.headers(), NUMBER_OF_FAILS)
        .map(Long::parseUnsignedLong);
  }

  public static <T> Optional<OffsetDateTime> creationTime(ConsumerRecord<String, T> message) {
    return headerAsString(message.headers(), CREATION_TIME)
        .map(Headers::parseOffsetDateTime);
  }

  public static <T> Optional<OffsetDateTime> lastFailTime(ConsumerRecord<String, T> message) {
    return headerAsString(message.headers(), LAST_FAIL_TIME)
        .map(Headers::parseOffsetDateTime);
  }

  static OffsetDateTime parseOffsetDateTime(String value) {
    return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  static Optional<String> headerAsString(org.apache.kafka.common.header.Headers headers, String key) {
    return Optional
        .ofNullable(headers.lastHeader(key))
        .map(Header::value)
        .map(value -> new String(value, StandardCharsets.UTF_8));
  }
}
