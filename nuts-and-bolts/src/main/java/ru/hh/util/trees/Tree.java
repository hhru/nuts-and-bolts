package ru.hh.util.trees;

import javax.annotation.concurrent.Immutable;

@Immutable
public interface Tree<COLOR, PAYLOAD, T extends Tree<COLOR, PAYLOAD, T>> {
  PAYLOAD get();

  T sub(COLOR id);

  Iterable<T> subs();
}
