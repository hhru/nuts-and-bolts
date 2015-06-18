package ru.hh.nab.health.limits;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public final class Limits {
  private final Map<String, Limit> limits;

  private static class CompoundLeaseToken implements LeaseToken {
    private final List<LeaseToken> subTokens;

    private CompoundLeaseToken(Iterable<LeaseToken> subTokens) {
      this.subTokens = ImmutableList.copyOf(subTokens);
    }

    @Override
    public void release() {
      for (LeaseToken t : subTokens) {
        t.release();
      }
    }
  }

  public Limits(Map<String, Limit> limits) {
    this.limits = ImmutableMap.copyOf(limits);
  }

  public LeaseToken acquire(String... limitNames) {
    List<LeaseToken> tokens = Lists.newArrayList();
    for (String limitName : limitNames) {
      Limit limit = limits.get(limitName);
      LeaseToken token = limit.acquire();
      if (token == null) {
        for (LeaseToken t : tokens) {
          t.release();
        }
        return null;
      }
      tokens.add(token);
    }
    return new CompoundLeaseToken(tokens);
  }

  public Limit compoundLimit(final String... limitNames) {
    return new Limit() {
      @Override
      public LeaseToken acquire() {
        return Limits.this.acquire(limitNames);
      }

      @Override
      public int getMax() {
        return -1;
      }

      @Override
      public String getName() {
        return String.format("compound<%s>", StringUtils.join(limitNames));
      }
    };
  }
}
