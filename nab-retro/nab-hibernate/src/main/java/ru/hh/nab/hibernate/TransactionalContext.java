package ru.hh.nab.hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import java.util.concurrent.Callable;

class TransactionalContext {

  private static final Logger logger = LoggerFactory.getLogger(TransactionalContext.class);

  private final EntityManager em;
  private EntityTransaction jpaTx = null; // null when not in transaction
  private PostCommitHooks postCommitHooks = new PostCommitHooks();

  TransactionalContext(EntityManager em) {
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
  }

  <T> T runInTransaction(Transactional ann, Callable<T> invocation, PostCommitHooks postCommitHooks) throws Exception {
    PostCommitHooks savedCommitHooks = this.postCommitHooks;
    this.postCommitHooks = postCommitHooks;
    try {
      begin(ann);
      final T result;
      try {
        result = invocation.call();
      } catch (Exception e) {
        try {
          maybeRollback();
        } catch (RuntimeException rollbackE) {
          logger.debug(rollbackE.getMessage());
        }
        throw e;
      }
      end();
      return result;
    } finally {
      this.postCommitHooks = savedCommitHooks;
      resetJpaTransaction();
    }
  }

  boolean inTransaction() {
    return jpaTx != null;
  }

  private void begin(Transactional ann) {
    jpaTx = em.getTransaction();
    em.setFlushMode(FlushModeType.AUTO);
    if (ann.rollback()) {
      jpaTx.setRollbackOnly();
    }
    jpaTx.begin();
  }

  private void end() {
    if (jpaTx.getRollbackOnly()) {
      jpaTx.rollback();
    } else {
      jpaTx.commit();
    }
  }

  private void maybeRollback() {
    if (jpaTx.isActive()) {
      jpaTx.rollback();
    }
  }

  private void resetJpaTransaction() {
    jpaTx = null;
    em.setFlushMode(FlushModeType.AUTO);
  }
}
