package ru.hh.nab.jersey;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;

public class HttpRequestMDCDecorator implements MethodInterceptor {
  private final Provider<Injector> inj;
  private static final String X_REQUEST_ID = "x-request-id";
  private static final String X_HHID_PERFORMER = "x-hhid-performer";
  private static final String X_UID = "x-uid";
  private static final String REQ_REMOTE_ADDR = "req.remote-addr";

  public HttpRequestMDCDecorator(Provider<Injector> inj) {
    this.inj = inj;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    HttpRequestContext req = inj.get().getInstance(HttpContext.class).getRequest();

    GrizzlyRequest grizzlyReq = Preconditions.checkNotNull(
            NabGrizzlyContainer.CURRENT_REQUEST.get());

    storeHeaderValue(req, X_REQUEST_ID);
    storeHeaderValue(req, X_HHID_PERFORMER);
    storeHeaderValue(req, X_UID);
    MDC.put(REQ_REMOTE_ADDR, grizzlyReq.getRemoteAddr());

    try {
      return invocation.proceed();
    } finally {
      removeHeaderValue(X_REQUEST_ID);
      removeHeaderValue(X_HHID_PERFORMER);
      removeHeaderValue(X_UID);
      MDC.remove(REQ_REMOTE_ADDR);
    }
  }

  private void storeHeaderValue(HttpRequestContext req, String header) {
    MDC.put("req.h." + header, req.getHeaderValue(header));
  }

  private void removeHeaderValue(String header) {
    MDC.remove("req.h." + header);
  }
}
