package ru.hh.nab.hibernate;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TxInterceptor implements MethodInterceptor {
  private static class CurrentTx {
    private final EntityManager em;
    private boolean readOnly;
    private final EntityTransaction tx;

    public CurrentTx(EntityManager em, Transactional ann) {
      this.em = em;
      this.tx = em.getTransaction();
      this.readOnly = ann.readOnly();
      em.setFlushMode(readOnly ? FlushModeType.COMMIT : FlushModeType.AUTO);
      if (ann.rollback())
        tx.setRollbackOnly();
    }

    public void enter(Transactional ann) {
      if (!tx.getRollbackOnly() && ann.rollback()) {
        throw new IllegalStateException("Can't execute (rollback() == true) tx while in (rollback() == false) tx");
      }
      if (readOnly && !ann.readOnly()) {
        em.setFlushMode(FlushModeType.AUTO);
        readOnly = false;
      }
    }

    public void begin() {
      tx.begin();
    }

    public void commit() {
      tx.commit();
    }

    public void rollbackIfActive() {
      if (tx.isActive())
        tx.rollback();
    }
  }

  private ThreadLocal<CurrentTx> tx = new ThreadLocal<CurrentTx>();

  private Provider<EntityManagerFactory> emf;

  public TxInterceptor(Provider<EntityManagerFactory> emf) {
    this.emf = emf;
  }

  public <T> T invoke(Transactional ann, Callable<T> invocation) throws Exception {
    EntityManager em = emf.get().createEntityManager();
    CurrentTx tx = this.tx.get();
    if (tx != null) {
      tx.enter(ann);
      return invocation.call();
    }
    try {
      tx = new CurrentTx(em, ann);
      this.tx.set(tx);
      T result;
      tx.begin();
      try {
        result = invocation.call();
        tx.commit();
      } catch (Exception e) {
        tx.rollbackIfActive();
        throw e;
      }
      return result;
    } finally {
      this.tx.remove();
      em.close();
    }
  }

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    return invoke(invocation.getMethod().getAnnotation(Transactional.class),
            new Callable<Object>() {
              @Override
              public Object call() throws Exception {
                try {
                  return invocation.proceed();
                } catch (Throwable throwable) {
                  if (throwable instanceof Exception)
                    throw (Exception)throwable;
                  else
                    throw new RuntimeException(throwable);
                }
              }
            });
  }

  public EntityManager currentEntityManager() {
    CurrentTx tx = this.tx.get();
    Preconditions.checkState(tx != null, "Not in transaction");
    return tx.em;
  }
}
