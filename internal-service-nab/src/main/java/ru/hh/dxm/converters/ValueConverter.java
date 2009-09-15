package ru.hh.dxm.converters;

import javax.annotation.concurrent.Immutable;

@Immutable
public interface ValueConverter<T> {
  T fromString(String s);

  String toString(T v);
}
