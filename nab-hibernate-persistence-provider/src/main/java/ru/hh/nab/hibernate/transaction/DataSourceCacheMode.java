package ru.hh.nab.hibernate.transaction;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

public enum DataSourceCacheMode {

  NORMAL(CacheStoreMode.USE, CacheRetrieveMode.USE),
  GET(CacheStoreMode.BYPASS, CacheRetrieveMode.USE);

  private final CacheStoreMode storeMode;
  private final CacheRetrieveMode retrieveMode;

  DataSourceCacheMode(CacheStoreMode storeMode, CacheRetrieveMode retrieveMode) {
    this.storeMode = storeMode;
    this.retrieveMode = retrieveMode;
  }

  public CacheStoreMode getStoreMode() {
    return storeMode;
  }

  public CacheRetrieveMode getRetrieveMode() {
    return retrieveMode;
  }
}
