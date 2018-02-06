package ru.hh.nab.health.monitoring;

import javax.inject.Inject;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.scopes.RequestScope;
import java.lang.reflect.Method;

public class MethodProbingInterceptor implements MethodInterceptor {
  @Inject
  private static TimingsLoggerFactory timingsLoggerFactory;

  public static final MethodProbingInterceptor INSTANCE = new MethodProbingInterceptor();

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Method method = invocation.getMethod();
    Probe ann = method.getAnnotation(Probe.class);
    OutOfRequestScope outOfRequestScope = method.getAnnotation(OutOfRequestScope.class);

    TimingsLogger logger;

    if (outOfRequestScope != null) {
      RequestScope.checkNotInRequestScope();
      logger = timingsLoggerFactory.getLogger(invocation.getClass().getName(), outOfRequestScope.requestIdOverride());
    } else {
      logger = RequestScope.currentTimingsLogger();
    }

    logger.mark();
    try {
      return invocation.proceed();
    } finally {
      logger.probe(ann == null || ann.desc() == null ? invocation.getMethod().getName() : ann.desc());
    }
  }
}
