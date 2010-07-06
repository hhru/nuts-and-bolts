package ru.hh.nab.hibernate;

import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class TxInterceptor implements MethodInterceptor {
  private Provider<Session> session;

  public TxInterceptor(Provider<Session> session) {
    this.session = session;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Session currentSession = this.session.get();
    Transaction tx = currentSession.getTransaction();
    if (tx.isActive()) {
      return invocation.proceed();
    }
    Transactional ann = invocation.getMethod().getAnnotation(Transactional.class);
    Object result;
    tx.begin();
    FlushMode savedFlushMode = currentSession.getFlushMode();
    if (ann.readOnly()) {
      currentSession.setFlushMode(FlushMode.MANUAL);
    }
    try {
      result = invocation.proceed();
      if (ann.rollBack()) {
        tx.rollback();
      } else {
        tx.commit();
      }
    } catch (Throwable th) {
      tx.rollback();
      throw th;
    } finally {
      if (currentSession.isOpen() && ann.readOnly()) {
        currentSession.setFlushMode(savedFlushMode);
      }
    }
    return result;
  }
}
