package ru.hh.nab.security;

import com.google.inject.Provider;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.container.ContainerRequest;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class SecureInterceptor implements MethodInterceptor {
  private final Provider<HttpContext> req;

  public SecureInterceptor(Provider<HttpContext> req) {
    this.req = req;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    HttpContext r = req.get();

    Permissions permissions = (Permissions)r.getProperties()
            .get(SecurityFilter.REQUEST_PROPERTY_KEY);

    Secure ann = invocation.getMethod().getAnnotation(Secure.class);
    for (String p : ann.value()) {
      if (!permissions.hasPermissionTo(p))
        throw new UnauthorizedException();
    }
    return invocation.proceed();
  }
}
