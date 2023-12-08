package ru.hh.nab.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.kafka.common.serialization.Deserializer;

public interface DeserializerSupplier {

  <T> Deserializer<T> supplyFor(Class<T> clazz);

  <T> Deserializer<T> supplyFor(TypeReference<T> typeReference);
}
