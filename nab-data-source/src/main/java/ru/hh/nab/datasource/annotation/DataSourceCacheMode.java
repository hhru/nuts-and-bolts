package ru.hh.nab.datasource.annotation;

/**
 * Controls how the entityManager interacts with the second-level cache or query cache.
 * An instance of DataSourceCacheMode may be viewed as packaging a JPA-defined CacheStoreMode with a CacheRetrieveMode.
 */
public enum DataSourceCacheMode {

  /**
   * NORMAL represents the combination (CacheStoreMode.USE, CacheRetrieveMode.USE)
   * The entityManager may read items from the cache, and add items to the cache as it reads them from the database.
   */
  NORMAL,

  /**
   * IGNORE represents the combination (CacheStoreMode.BYPASS, CacheRetrieveMode.BYPASS)
   * The entityManager will never interact with the cache, except to invalidate cached items when updates occur.
   */
  IGNORE,

  /**
   * GET represents the combination (CacheStoreMode.BYPASS, CacheRetrieveMode.USE)
   * The entityManager may read items from the cache, but will not add items, except to invalidate items when updates occur.
   */
  GET,

  /**
   * PUT represents the combination (CacheStoreMode.USE, CacheRetrieveMode.BYPASS)
   * The entityManager will never read items from the cache, but will add items to the cache as it reads them from the database.
   * EntityManager does not force refresh of already cached items when reading from database.
   */
  PUT,

  /**
   * REFRESH represents the combination (CacheStoreMode.REFRESH, CacheRetrieveMode.BYPASS)
   * As with to PUT, the entityManager will never read items from the cache, but will add items to the cache as it reads them from the database.
   * EntityManager forces refresh of cache for items read from database.
   */
  REFRESH,
}
