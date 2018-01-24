package ru.hh.nab.filters;

import ru.hh.nab.util.MDC;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;

import static ru.hh.nab.util.MDC.CONTROLLER_MDC_KEY;

public class ResourceNameLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Inject
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String methodName = resourceInfo.getResourceClass().getSimpleName() + '.' + resourceInfo.getResourceMethod().getName();
    MDC.setKey(CONTROLLER_MDC_KEY, methodName);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.deleteKey(CONTROLLER_MDC_KEY);
  }
}
