package ru.hh.nab.hibernate.datasource.replica;

import org.hibernate.CacheMode;

public enum ReplicaCacheMode {

  NORMAL(CacheMode.NORMAL),
  GET(CacheMode.GET);

  private final CacheMode cacheMode;

  ReplicaCacheMode(CacheMode cacheMode) {
    this.cacheMode = cacheMode;
  }

  public CacheMode getHibernateCacheMode() {
    return cacheMode;
  }
}
