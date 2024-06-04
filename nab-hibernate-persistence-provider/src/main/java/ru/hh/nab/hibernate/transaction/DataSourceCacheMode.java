package ru.hh.nab.hibernate.transaction;

import org.hibernate.CacheMode;

public enum DataSourceCacheMode {

  NORMAL(CacheMode.NORMAL),
  GET(CacheMode.GET);

  private final CacheMode cacheMode;

  DataSourceCacheMode(CacheMode cacheMode) {
    this.cacheMode = cacheMode;
  }

  public CacheMode getHibernateCacheMode() {
    return cacheMode;
  }
}
