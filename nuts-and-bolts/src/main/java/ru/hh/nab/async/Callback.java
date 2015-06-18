package ru.hh.nab.async;

public interface Callback<T> {
  void call(T arg) throws Exception;
}
