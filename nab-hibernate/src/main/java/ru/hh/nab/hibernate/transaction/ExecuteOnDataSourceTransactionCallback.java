package ru.hh.nab.hibernate.transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class ExecuteOnDataSourceTransactionCallback implements TransactionCallback<Object> {
  
  private final ProceedingJoinPoint pjp;
  private final SessionFactory factory;
  private final ExecuteOnDataSource replica;

  ExecuteOnDataSourceTransactionCallback(ProceedingJoinPoint pjp, SessionFactory factory, ExecuteOnDataSource dataSource) {
    this.pjp = pjp;
    this.factory = factory;
    this.replica = dataSource;
  }

  @Override
  public Object doInTransaction(@NonNull TransactionStatus status) {
    Session session = null;
    CacheMode initialCacheMode = null;
    try {
      session = factory.getCurrentSession();
      initialCacheMode = session.getCacheMode();
      session.setCacheMode(replica.cacheMode().getHibernateCacheMode());

      return pjp.proceed();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new ExecuteOnDataSourceWrappedException(e);
    } finally {
      if (session != null && initialCacheMode != null) {
        session.setCacheMode(initialCacheMode);
      }
    }
  }
}
