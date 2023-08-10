package ru.hh.nab.starter.filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.starter.server.RequestHeaders;

@Provider
@ApplicationScoped
public final class RequestIdLoggingJaxRsFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String requestId = requestContext.getHeaderString(RequestHeaders.REQUEST_ID);
    if (requestId == null) {
      requestId = RequestHeaders.EMPTY_REQUEST_ID;
    }
    MDC.setRequestId(requestId);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    String requestId = requestContext.getHeaderString(RequestHeaders.REQUEST_ID);
    if (requestId != null) {
      responseContext.getHeaders().add(RequestHeaders.REQUEST_ID, requestId);
    }
    MDC.clearRequestId();
  }
}
