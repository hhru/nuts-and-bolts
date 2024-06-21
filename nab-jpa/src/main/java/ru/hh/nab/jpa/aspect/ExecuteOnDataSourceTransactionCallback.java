package ru.hh.nab.jpa.aspect;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import ru.hh.nab.jdbc.annotation.DataSourceCacheMode;
import ru.hh.nab.jdbc.annotation.ExecuteOnDataSource;
import ru.hh.nab.jdbc.aspect.ExecuteOnDataSourceWrappedException;

public class ExecuteOnDataSourceTransactionCallback implements TransactionCallback<Object> {

  private static final String CACHE_STORE_MODE_PROPERTY = "jakarta.persistence.cache.storeMode";
  private static final String CACHE_RETRIEVE_MODE_PROPERTY = "jakarta.persistence.cache.retrieveMode";
  
  private final ProceedingJoinPoint pjp;
  private final EntityManager entityManager;
  private final ExecuteOnDataSource executeOnDataSource;

  ExecuteOnDataSourceTransactionCallback(ProceedingJoinPoint pjp, EntityManager entityManager, ExecuteOnDataSource executeOnDataSource) {
    this.pjp = pjp;
    this.entityManager = entityManager;
    this.executeOnDataSource = executeOnDataSource;
  }

  @Override
  public Object doInTransaction(@NonNull TransactionStatus status) {
    CacheStoreMode initialCacheStoreMode = null;
    CacheRetrieveMode initialCacheRetrieveMode = null;

    try {
      initialCacheStoreMode = (CacheStoreMode) entityManager.getProperties().get(CACHE_STORE_MODE_PROPERTY);
      initialCacheRetrieveMode = (CacheRetrieveMode) entityManager.getProperties().get(CACHE_RETRIEVE_MODE_PROPERTY);

      entityManager.setProperty(CACHE_STORE_MODE_PROPERTY, getCacheStoreMode(executeOnDataSource.cacheMode()));
      entityManager.setProperty(CACHE_RETRIEVE_MODE_PROPERTY, getCacheRetrieveMode(executeOnDataSource.cacheMode()));

      return pjp.proceed();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new ExecuteOnDataSourceWrappedException(e);
    } finally {
      if (initialCacheStoreMode != null) {
        entityManager.setProperty(CACHE_STORE_MODE_PROPERTY, initialCacheStoreMode);
      }
      if (initialCacheRetrieveMode != null) {
        entityManager.setProperty(CACHE_RETRIEVE_MODE_PROPERTY, initialCacheRetrieveMode);
      }
    }
  }

  private CacheStoreMode getCacheStoreMode(DataSourceCacheMode cacheMode) {
    return switch (cacheMode) {
      case NORMAL -> CacheStoreMode.USE;
      case GET -> CacheStoreMode.BYPASS;
    };
  }

  private CacheRetrieveMode getCacheRetrieveMode(DataSourceCacheMode cacheMode) {
    return switch (cacheMode) {
      case NORMAL, GET -> CacheRetrieveMode.USE;
    };
  }
}