package ru.hh.nab.async;

import java.lang.Exception;

public interface Callback<T> {
  void call(T arg) throws Exception;
}