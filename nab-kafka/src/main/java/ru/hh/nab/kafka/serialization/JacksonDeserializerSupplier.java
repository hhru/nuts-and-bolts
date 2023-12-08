package ru.hh.nab.kafka.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.hh.nab.kafka.consumer.DeserializerSupplier;

public class JacksonDeserializerSupplier implements DeserializerSupplier {

  private final ObjectMapper objectMapper;

  public JacksonDeserializerSupplier(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public <T> Deserializer<T> supplyFor(Class<T> clazz) {
    return new JsonDeserializer<>(clazz, objectMapper, false);
  }

  @Override
  public <T> Deserializer<T> supplyFor(TypeReference<T> typeReference) {
    return new JsonDeserializer<>(typeReference, objectMapper, false);
  }
}
