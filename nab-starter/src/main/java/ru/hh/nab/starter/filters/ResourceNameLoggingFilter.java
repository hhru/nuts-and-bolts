package ru.hh.nab.starter.filters;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import org.springframework.util.ClassUtils;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CODE_FUNCTION_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.CODE_NAMESPACE_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;

public class ResourceNameLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Inject
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Class<?> controllerClass = ClassUtils.getUserClass(resourceInfo.getResourceClass());
    String resourceMethodName = resourceInfo.getResourceMethod().getName();
    String controller = controllerClass.getSimpleName() + '#' + resourceMethodName;
    requestContext.setProperty(CONTROLLER_MDC_KEY, controller);
    requestContext.setProperty(CODE_FUNCTION_MDC_KEY, resourceMethodName);
    requestContext.setProperty(CODE_NAMESPACE_MDC_KEY, controllerClass.getName());
    MDC.setController(controller);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.clearController();
  }
}
