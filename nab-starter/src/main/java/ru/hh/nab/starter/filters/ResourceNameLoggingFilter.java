package ru.hh.nab.starter.filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;
import org.springframework.util.ClassUtils;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;

@Provider
@ApplicationScoped
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
