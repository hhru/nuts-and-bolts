package ru.hh.nab.kafka.consumer.retry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

public class HeadersMessageMetadataProvider {
  public static final String HEADER_MESSAGE_PROCESSING_HISTORY = "x-retry-message-processing-history";
  public static final String HEADER_NEXT_RETRY_TIME = "x-retry-next-retry-time";
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .registerModule(new JavaTimeModule());

  public static Optional<MessageProcessingHistory> getMessageProcessingHistory(Headers headers) {
    return Optional
        .ofNullable(headers.lastHeader(HEADER_MESSAGE_PROCESSING_HISTORY))
        .map(Header::value)
        .map(value -> {
          try {
            return objectMapper.readValue(value, MessageProcessingHistory.class);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public static Optional<Instant> getNextRetryTime(Headers headers) {
    return Optional
        .ofNullable(headers.lastHeader(HEADER_NEXT_RETRY_TIME))
        .map(Header::value)
        .map(value -> Instant.parse(new String(value, StandardCharsets.UTF_8)));
  }

  public static void setMessageProcessingHistory(Headers headers, MessageProcessingHistory messageProcessingHistory) {
    byte[] headerValue;
    try {
      headerValue = objectMapper.writeValueAsBytes(messageProcessingHistory);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    headers
        .remove(HEADER_MESSAGE_PROCESSING_HISTORY)
        .add(HEADER_MESSAGE_PROCESSING_HISTORY, headerValue);
  }

  public static void setNextRetryTime(Headers headers, Instant nextRetryTime) {
    byte[] headerValue = nextRetryTime.toString().getBytes(StandardCharsets.UTF_8);
    headers
        .remove(HEADER_NEXT_RETRY_TIME)
        .add(HEADER_NEXT_RETRY_TIME, headerValue);
  }
}
