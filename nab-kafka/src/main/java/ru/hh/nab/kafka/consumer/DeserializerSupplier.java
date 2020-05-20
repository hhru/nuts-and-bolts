package ru.hh.nab.kafka.consumer;

import org.apache.kafka.common.serialization.Deserializer;

@FunctionalInterface
public interface DeserializerSupplier {

  <T> Deserializer<T> supplyFor(Class<T> clazz);

}
