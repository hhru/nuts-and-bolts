package ru.hh.nab.health.limits;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public final class Limits {
  private final Map<String, Limit> limits;

  public Limits(Map<String, Limit> limits) {
    this.limits = ImmutableMap.copyOf(limits);
  }

  public boolean acquire(String... limitNames) {
    for (int i = 0; i < limitNames.length; i++) {
      Limit limit = limits.get(limitNames[i]);
      if (!limit.acquire()) {
        for (i--; i >= 0; i--) {
          limit = limits.get(limitNames[i]);
          limit.release();
        }
        return false;
      }
    }
    return true;
  }

  public void release(String... limitNames) {
    for (String limitName: limitNames) {
      limits.get(limitName).release();
    }
  }

  public Limit compoundLimit(final String... limitNames) {
    return new Limit() {
      @Override
      public boolean acquire() {
        return Limits.this.acquire(limitNames);
      }

      @Override
      public void release() {
        Limits.this.release(limitNames);
      }
    };
  }
}
