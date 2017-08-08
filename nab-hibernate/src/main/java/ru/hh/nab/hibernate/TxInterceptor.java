package ru.hh.nab.hibernate;

import java.util.concurrent.Callable;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TxInterceptor implements MethodInterceptor {

  private final ThreadLocal<TransactionalContext> txContextHolder = new ThreadLocal<>();
  private final Provider<EntityManagerFactory> entityManagerFactoryProvider;

  public TxInterceptor(Provider<EntityManagerFactory> entityManagerFactoryProvider) {
    this.entityManagerFactoryProvider = entityManagerFactoryProvider;
  }

  public <T> T invoke(Transactional ann, Callable<T> invocation) throws Exception {
    TransactionalContext txContext = txContextHolder.get();
    // Is transaction context already initialized (i.e. have we already
    // encountered Transactional annotation) ?
    if (txContext != null) {
      if (txContext.inTransaction()) {
        // continue previously started transaction
        txContext.enter(ann);
        return invocation.call();
      } else if (ann.readOnly()) {
        // not in transaction, and no need to start transaction
        return invocation.call();
      } else {
        // not in transaction, need to start transaction
        return runInTransaction(ann, invocation);
      }
    }

    EntityManager em = null;
    try {
      // create entity manager instance and init new context
      em = entityManagerFactoryProvider.get().createEntityManager();
      txContext = new TransactionalContext(em);
      txContextHolder.set(txContext);

      // call the callback...
      if (ann.readOnly()) {
        // ...without transaction
        return runWithoutTransaction(invocation);
      } else {
        // ...with transaction
        return runInTransaction(ann, invocation);
      }
    } finally {
      // release entity manager and remove transaction context object
      // if we have created them in this call
      if (em != null) {
        txContextHolder.remove();
        em.close();
      }
    }
  }

  private <T> T runInTransaction(Transactional ann, Callable<T> invocation) throws Exception {
    TransactionalContext tx = txContextHolder.get();
    PostCommitHooks hooks = new PostCommitHooks();
    T result = tx.runInTransaction(ann, invocation, hooks);
    try {
      // post commit hooks must know nothing about
      // current transaction (or have access to entity manager)
      txContextHolder.remove();
      hooks.execute();
    } finally {
      txContextHolder.set(tx);
    }
    return result;
  }

  private <T> T runWithoutTransaction(Callable<T> invocation) throws Exception {
    TransactionalContext tx = txContextHolder.get();
    T result = invocation.call();
    try {
      // post commit hooks must know nothing about
      // current transaction (or have access to entity manager)
      txContextHolder.remove();
      tx.getPostCommitHooks().execute();
    } finally {
      txContextHolder.set(tx);
    }
    return result;
  }

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    return invoke(
      invocation.getMethod().getAnnotation(Transactional.class),
        () -> {
          try {
            return invocation.proceed();
          } catch (Exception exception) {
            throw exception;
          } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
          }
        }
    );
  }

  public EntityManager currentEntityManager() {
    TransactionalContext tx = txContextHolder.get();
    return tx.getEntityManager();
  }

  public PostCommitHooks currentPostCommitHooks() {
    TransactionalContext tx = txContextHolder.get();
    return tx.getPostCommitHooks();
  }
}
