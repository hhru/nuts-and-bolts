package ru.hh.nab.async;

public interface OnErrorCallback<E extends Exception> {
  void call(Throwable arg) throws E;
}
