package ru.hh.nab.kafka.consumer.retry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;

public class HeadersMessageMetadataProvider implements MessageMetadataProvider {
  public static final String HEADER_MESSAGE_PROCESSING_HISTORY = "x-kafka-retry-message-processing-history";
  public static final String HEADER_NEXT_RETRY_TIME = "x-kafka-retry-next-retry_time";
  ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Override
  public Optional<MessageProcessingHistory> getMessageProcessingHistory(ConsumerRecord<?, ?> consumerRecord) {
    return Optional
        .ofNullable(consumerRecord.headers().lastHeader(HEADER_MESSAGE_PROCESSING_HISTORY))
        .map(Header::value)
        .map(value -> {
          try {
            return objectMapper.readValue(value, MessageProcessingHistory.class);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public void setMessageProcessingHistory(ProducerRecord<?, ?> producerRecord, MessageProcessingHistory messageProcessingHistory) {
    byte[] headerValue;
    try {
      headerValue = objectMapper.writeValueAsBytes(messageProcessingHistory);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    producerRecord
        .headers()
        .remove(HEADER_MESSAGE_PROCESSING_HISTORY)
        .add(HEADER_MESSAGE_PROCESSING_HISTORY, headerValue);
  }

  @Override
  public Optional<Instant> getNextRetryTime(ConsumerRecord<?, ?> consumerRecord) {
    return Optional
        .ofNullable(consumerRecord.headers().lastHeader(HEADER_NEXT_RETRY_TIME))
        .map(Header::value)
        .map(value -> Instant.parse(new String(value, StandardCharsets.UTF_8)));
  }

  @Override
  public void setNextRetryTime(ProducerRecord<?, ?> producerRecord, Instant nextRetryTime) {
    byte[] headerValue = nextRetryTime.toString().getBytes(StandardCharsets.UTF_8);
    producerRecord
        .headers()
        .remove(HEADER_MESSAGE_PROCESSING_HISTORY)
        .add(HEADER_MESSAGE_PROCESSING_HISTORY, headerValue);
  }
}
