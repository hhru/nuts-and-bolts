package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class NginxQuirksFilter implements ResourceFilter {
  public static class NginxQuirksResponseFilter implements ContainerResponseFilter {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
      if (response.getEntity() == null) {
        response.setEntity(" ".getBytes());
      }
      return response;
    }
  }

  private static final NginxQuirksResponseFilter FILTER_INSTANCE = new NginxQuirksResponseFilter();

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return null;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return FILTER_INSTANCE;
  }
}
