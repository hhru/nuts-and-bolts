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
    private boolean readOnly = false;
    private EntityTransaction tx = null; // null for dummy transactions
    private final PostCommitHooks postCommitHooks = new PostCommitHooks();

    public CurrentTx(EntityManager em, Transactional ann) {
      this.em = em;
      initTx(em, ann);
    }

    private void initTx(EntityManager em, Transactional ann) {
      if (!ann.dummy()) {
        readOnly = ann.readOnly();
        tx = em.getTransaction();
        em.setFlushMode(readOnly ? FlushModeType.COMMIT : FlushModeType.AUTO);
        if (ann.rollback()) {
          tx.setRollbackOnly();
        }
      } else {
        initDummy();
      }
    }

    public void enter(Transactional ann) {
      if (isDummy()) {
        initTx(em, ann);
      } else {
        if (!tx.getRollbackOnly() && ann.rollback()) {
          throw new IllegalStateException("Can't execute (rollback() == true) tx while in (rollback() == false) tx");
        }
        if (readOnly && !ann.readOnly()) {
          em.setFlushMode(FlushModeType.AUTO);
          readOnly = false;
        }
      }
    }

    public void begin() {
      if (isDummy()) {
        throw new IllegalStateException("begin() must not be called in a dummy transaction");
      }
      tx.begin();
    }

    public void commit() {
      if (isDummy()) {
        throw new IllegalStateException("commit() must not be called in a dummy transaction");
      }
      tx.commit();
      initDummy();
    }

    public void rollbackIfActive() {
      if (isDummy()) {
        throw new IllegalStateException("rollbackIfActive() must not be called in a dummy transaction");
      }
      if (tx.isActive()) {
        tx.rollback();
      }
      initDummy();
    }

    public boolean isDummy() {
      return tx == null;
    }

    public void initDummy() {
      tx = null;
      em.setFlushMode(FlushModeType.AUTO);
    }
  }

  private ThreadLocal<CurrentTx> tx = new ThreadLocal<CurrentTx>();

  private Provider<EntityManagerFactory> emf;

  public TxInterceptor(Provider<EntityManagerFactory> emf) {
    this.emf = emf;
  }

  public <T> T invoke(Transactional ann, Callable<T> invocation) throws Exception {
    EntityManager em = null;
    boolean committed = false;
    CurrentTx tx = this.tx.get();
    try {
      if (tx == null) {
        // init new 'dummy' or 'real' transaction
        em = emf.get().createEntityManager();
        tx = new CurrentTx(em, ann);
        this.tx.set(tx);
      } else if (!tx.isDummy()) {
        // continue previously started 'real' transaction
        tx.enter(ann);
        return invocation.call();
      }

      // continue or start 'dummy' transaction
      if (ann.dummy()) {
        return invocation.call();
      }

      // start new 'real' transaction, possibly over previous 'dummy' transaction
      T result;
      if (tx.isDummy()) {
        // override dummy transaction bits
        tx.enter(ann);
      }
      tx.begin();
      try {
        result = invocation.call();
        tx.commit();
        committed = true;
      } catch (Exception e) {
        tx.rollbackIfActive();
        throw new Exception(e);
      }
      return result;
    } finally {
      // release entity manager and remove transaction if we have created it in this call
      if (em != null) {
        this.tx.remove();
        em.close();
      }
      if (committed)
        tx.postCommitHooks.execute();
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

  public PostCommitHooks currentPostCommitHooks() {
    CurrentTx tx = this.tx.get();
    Preconditions.checkState(tx != null, "Not in transaction");
    return tx.postCommitHooks;
  }
}
