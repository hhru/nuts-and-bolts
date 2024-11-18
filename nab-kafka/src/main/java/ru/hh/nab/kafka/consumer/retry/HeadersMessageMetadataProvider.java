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

/**
 * Utility to store and extract retries related metadata in message headers.
 * <p>
 * Next scheduled retry time is stored in {@link #HEADER_NEXT_RETRY_TIME} using {@link Instant#toString()}
 * <p>
 * {@link MessageProcessingHistory} is stored in {@link #HEADER_MESSAGE_PROCESSING_HISTORY}
 * using simple JSON serialization:
 * <pre>
 *   {
 *     "creationTime" : 1.730211537913E9,
 *     "retryNumber" : 1,
 *     "lastFailTime" : 1.7302115379555104E9
 *   }
 * </pre>
 * */
public class HeadersMessageMetadataProvider {
  public static final String HEADER_MESSAGE_PROCESSING_HISTORY = "x-retry-message-processing-history";
  public static final String HEADER_NEXT_RETRY_TIME = "x-retry-next-retry-time";
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .registerModule(new JavaTimeModule());

  /**
   * Get {@link MessageProcessingHistory} from message headers
   * */
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

  /**
   * Get next scheduled retry time from message headers
   * */
  public static Optional<Instant> getNextRetryTime(Headers headers) {
    return Optional
        .ofNullable(headers.lastHeader(HEADER_NEXT_RETRY_TIME))
        .map(Header::value)
        .map(value -> Instant.parse(new String(value, StandardCharsets.UTF_8)));
  }

  /**
   * Store {@link MessageProcessingHistory} to message headers
   * */
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

  /**
   * Store next scheduled retry time to message headers
   * */
  public static void setNextRetryTime(Headers headers, Instant nextRetryTime) {
    byte[] headerValue = nextRetryTime.toString().getBytes(StandardCharsets.UTF_8);
    headers
        .remove(HEADER_NEXT_RETRY_TIME)
        .add(HEADER_NEXT_RETRY_TIME, headerValue);
  }
}
