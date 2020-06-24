package ru.hh.nab.kafka.producer;

import org.apache.kafka.common.serialization.Serializer;

@FunctionalInterface
public interface SerializerSupplier {

  <T> Serializer<T> supply();
}
