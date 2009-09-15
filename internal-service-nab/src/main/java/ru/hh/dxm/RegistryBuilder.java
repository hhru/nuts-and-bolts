package ru.hh.dxm;

import com.google.common.collect.Maps;
import java.util.Map;

public class RegistryBuilder {
  private Map<Class<?>, Marshaller<?>> marshallers = Maps.newHashMap();

  public RegistryBuilder with(Class<?> klass, Marshaller<?> marshaller) {
    marshallers.put(klass, marshaller);
    return this;
  }

  public MarshallersRegistry build() {
    return new MarshallersRegistry(marshallers);
  }
}
