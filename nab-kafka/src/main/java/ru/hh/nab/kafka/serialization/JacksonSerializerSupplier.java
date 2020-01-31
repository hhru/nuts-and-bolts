package ru.hh.nab.kafka.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.hh.nab.kafka.producer.SerializerSupplier;

public class JacksonSerializerSupplier implements SerializerSupplier {

  private final ObjectMapper objectMapper;

  public JacksonSerializerSupplier(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public <T> Serializer<T> supply() {
    JsonSerializer<T> serializer = new JsonSerializer<>(objectMapper);
    serializer.setAddTypeInfo(false);
    return serializer;
  }
}
