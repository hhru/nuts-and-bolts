package ru.hh.nab.hibernate.datasource.replica;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class ReplicaTransactionCallback implements TransactionCallback<Object> {
  
  private final ProceedingJoinPoint pjp;
  private final SessionFactory factory;
  private final ExecuteOnReplica replica;

  ReplicaTransactionCallback(final ProceedingJoinPoint pjp, final SessionFactory factory, final ExecuteOnReplica replica) {
    this.pjp = pjp;
    this.factory = factory;
    this.replica = replica;
  }

  @Override
  public Object doInTransaction(final TransactionStatus status) {
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
      throw new RuntimeException(e);
    } finally {
      if (session != null && initialCacheMode != null) {
        session.setCacheMode(initialCacheMode);
      }
    }
  }
}
