package ru.hh.nab.starter.filters;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import org.springframework.util.ClassUtils;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;

public class ResourceNameLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Inject
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String methodName = ClassUtils.getUserClass(resourceInfo.getResourceClass()).getSimpleName() + '#' + resourceInfo.getResourceMethod().getName();
    requestContext.setProperty(CONTROLLER_MDC_KEY, methodName);
    MDC.setController(methodName);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.clearController();
  }
}
