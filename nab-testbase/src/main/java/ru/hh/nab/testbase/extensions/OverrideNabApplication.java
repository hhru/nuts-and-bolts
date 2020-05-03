package ru.hh.nab.testbase.extensions;

import ru.hh.nab.starter.NabApplication;

public interface OverrideNabApplication {
  default NabApplication getNabApplication() {
    return NabApplication.builder().build();
  }
}
