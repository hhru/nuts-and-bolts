package ru.hh.nab.kafka.producer;

import org.apache.kafka.common.serialization.Serializer;

public interface SerializerSupplier {

  <T> Serializer<T> supply();
}
