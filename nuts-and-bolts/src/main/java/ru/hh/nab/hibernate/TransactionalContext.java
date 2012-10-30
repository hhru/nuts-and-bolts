package ru.hh.nab.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import java.util.concurrent.Callable;

class TransactionalContext {
  private final EntityManager em;
  private boolean readOnly = false;
  private EntityTransaction jpaTx = null; // null when not in transaction
  private PostCommitHooks postCommitHooks = new PostCommitHooks();

  public TransactionalContext(EntityManager em) {
    this.em = em;
  }

  EntityManager getEntityManager() {
    return em;
  }

  PostCommitHooks getPostCommitHooks() {
    return postCommitHooks;
  }

  public void enter(Transactional ann) {
    if (!jpaTx.getRollbackOnly() && ann.rollback()) {
      throw new IllegalStateException("Can't execute (rollback() == true) tx while in (rollback() == false) tx");
    }
    if (readOnly && !ann.readOnly()) {
      em.setFlushMode(FlushModeType.AUTO);
      readOnly = false;
    }
  }

  public <T> T runInTransaction(Transactional ann, Callable<T> invocation, PostCommitHooks postCommitHooks) throws Exception {
    PostCommitHooks savedCommitHooks = this.postCommitHooks;
    this.postCommitHooks = postCommitHooks;
    try {
      begin(ann);
      T result = invocation.call();
      commit();
      return result;
    } catch (Exception e) {
      rollbackIfActive();
      throw e;
    } finally {
      this.postCommitHooks = savedCommitHooks;
      resetJpaTransaction();
    }
  }

  boolean inTransaction() {
    return jpaTx != null;
  }

  private void begin(Transactional ann) {
    readOnly = ann.readOnly();
    jpaTx = em.getTransaction();
    em.setFlushMode(readOnly ? FlushModeType.COMMIT : FlushModeType.AUTO);
    if (ann.rollback()) {
      jpaTx.setRollbackOnly();
    }
    jpaTx.begin();
  }

  private void commit() {
    jpaTx.commit();
  }

  private void rollbackIfActive() {
    if (jpaTx.isActive()) {
      jpaTx.rollback();
    }
  }

  private void resetJpaTransaction() {
    jpaTx = null;
    em.setFlushMode(FlushModeType.AUTO);
  }
}
