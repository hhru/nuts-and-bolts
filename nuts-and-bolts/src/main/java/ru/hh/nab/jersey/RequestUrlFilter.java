package ru.hh.nab.jersey;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import org.slf4j.MDC;

public class RequestUrlFilter implements ResourceFilter {
  public static final String CONTROLLER_MDC_KEY = "controller";
  private final String methodName;

  public RequestUrlFilter(AbstractMethod abstractMethod) {
    methodName = abstractMethod.getMethod().getDeclaringClass().getSimpleName() + '.' + abstractMethod.getMethod().getName();
  }

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return request -> {
      MDC.put(CONTROLLER_MDC_KEY, methodName);
      return request;
    };
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return (request, response) -> {
      MDC.remove(CONTROLLER_MDC_KEY);
      return response;
    };
  }
}
