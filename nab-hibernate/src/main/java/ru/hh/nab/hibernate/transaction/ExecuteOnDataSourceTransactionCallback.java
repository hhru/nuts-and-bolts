package ru.hh.nab.hibernate.transaction;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.lang.NonNull;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class ExecuteOnDataSourceTransactionCallback implements TransactionCallback<Object> {

  private static final String CACHE_STORE_MODE_PROPERTY = "jakarta.persistence.cache.storeMode";
  private static final String CACHE_RETRIEVE_MODE_PROPERTY = "jakarta.persistence.cache.retrieveMode";
  
  private final ProceedingJoinPoint pjp;
  private final EntityManagerProxy entityManagerProxy;
  private final DataSourceCacheMode cacheMode;

  ExecuteOnDataSourceTransactionCallback(ProceedingJoinPoint pjp, EntityManagerProxy entityManagerProxy, DataSourceCacheMode cacheMode) {
    this.pjp = pjp;
    this.entityManagerProxy = entityManagerProxy;
    this.cacheMode = cacheMode;
  }

  @Override
  public Object doInTransaction(@NonNull TransactionStatus status) {
    EntityManager entityManager = entityManagerProxy.getTargetEntityManager();

    CacheStoreMode initialCacheStoreMode = null;
    CacheRetrieveMode initialCacheRetrieveMode = null;

    try {
      initialCacheStoreMode = (CacheStoreMode) entityManager.getProperties().get(CACHE_STORE_MODE_PROPERTY);
      initialCacheRetrieveMode = (CacheRetrieveMode) entityManager.getProperties().get(CACHE_RETRIEVE_MODE_PROPERTY);

      entityManager.setProperty(CACHE_STORE_MODE_PROPERTY, cacheMode.getStoreMode());
      entityManager.setProperty(CACHE_RETRIEVE_MODE_PROPERTY, cacheMode.getRetrieveMode());

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
}
