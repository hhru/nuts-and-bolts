package ru.hh.nab.health.monitoring;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ru.hh.nab.scopes.RequestScope;

public class MethodProbeInterceptor implements MethodInterceptor {
  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    RequestScope.currentTimingsLogger().probe(invocation.getMethod().toString());
    return invocation.proceed();
  }
}
