package ru.hh.dxm;

import javax.annotation.concurrent.Immutable;

@Immutable
public interface Detectable {
  String topElement();

  char[] topElementAsChars();
}
