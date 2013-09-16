package ru.hh.nab.health.monitoring;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.scopes.RequestScope;
import java.lang.reflect.Method;

public class MethodProbingInterceptor implements MethodInterceptor {

  public static final MethodProbingInterceptor INSTANCE = new MethodProbingInterceptor();

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Method method = invocation.getMethod();
    Probe ann = method.getAnnotation(Probe.class);
    TimingsLogger logger = RequestScope.currentTimingsLogger();
    if (ann == null || ann.desc() == null) {
      logger.probe(invocation.getMethod().toString());
    } else {
      logger.probe(ann.desc());
    }
    return invocation.proceed();
  }
}
