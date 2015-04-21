package ru.hh.nab.grizzly.monitoring;


import javax.annotation.Nullable;

public interface MarkableProbe {
  
  void mark(@Nullable String requestId, String anchor);
}
