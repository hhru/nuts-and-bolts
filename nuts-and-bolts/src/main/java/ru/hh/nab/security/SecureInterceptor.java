package ru.hh.nab.security;

import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class SecureInterceptor implements MethodInterceptor {
  private final Provider<Permissions> permissions;

  public SecureInterceptor(Provider<Permissions> permissions) {
    this.permissions = permissions;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Permissions perms = permissions.get();

    Secure ann = invocation.getMethod().getAnnotation(Secure.class);
    for (String p : ann.value()) {
      if (!perms.hasPermissionTo(p))
        throw new UnauthorizedException();
    }
    return invocation.proceed();
  }
}
