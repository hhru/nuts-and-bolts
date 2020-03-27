package ru.hh.nab.starter.common;

public interface Visitor<T> {
  void visit(T object);
}
