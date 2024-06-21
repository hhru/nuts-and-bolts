package ru.hh.nab.hibernate.transaction;

/**
 * Controls how the entityManager interacts with the second-level cache or query cache.
 * An instance of DataSourceCacheMode may be viewed as packaging a JPA-defined CacheStoreMode with a CacheRetrieveMode.
 */
public enum DataSourceCacheMode {

  /**
   * NORMAL represents the combination (CacheStoreMode.USE, CacheRetrieveMode.USE)
   */
  NORMAL,

  /**
   * GET represents the combination (CacheStoreMode.BYPASS, CacheRetrieveMode.USE)
   */
  GET,
}
