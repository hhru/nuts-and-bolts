package ru.hh.nab.jersey;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.health.limits.LeaseToken;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.Limits;
import ru.hh.nab.scopes.RequestScope;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import java.lang.reflect.Method;

public class ConcurrentJerseyMethodInterceptor implements MethodInterceptor {
  private final Provider<Limits> limits;

  private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentJerseyMethodInterceptor.class);

  public ConcurrentJerseyMethodInterceptor(Provider<Limits> limits) {
    this.limits = limits;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    final Method method = invocation.getMethod();
    final Concurrency concurrency = method.getAnnotation(Concurrency.class);
    if (concurrency.value().length == 0) {
      throw new IllegalArgumentException("Concurrency annotation must have non-empty value specified");
    }
    final Limit limit = limits.get().compoundLimit(concurrency.value());

    final LeaseToken leaseToken = limit.acquire();
    if (leaseToken == null) {
      LOGGER.warn("Failed to acquire limit, too many requests, responding with 503");
      throw new WebApplicationException(503);
    }

    RequestScope.addAfterServiceTask(() -> {
      leaseToken.release();
      return null;
    });

    return invocation.proceed();
  }
}
