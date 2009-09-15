package ru.hh.dxm;

public interface Marshallers {
  <T> Marshaller<T> marshaller(Class<T> klass);
}
