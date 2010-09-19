package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class FreemarkerModelFilter implements ResourceFilter {
  private static final ContainerResponseFilter INSTANCE = new ContainerResponseFilter() {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
      Object entity = response.getEntity();
      if (entity == null)
        return response;
      if (entity.getClass().getAnnotation(FreemarkerTemplate.class) != null) {
        response.setEntity(new FreemarkerModel(request.getProperties(), entity));
      }
      return response;
    }
  };

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return null;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return INSTANCE;
  }
}
