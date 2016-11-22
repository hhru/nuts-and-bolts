package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class NginxQuirksFilter implements ResourceFilter {
  public static class NginxQuirksResponseFilter implements ContainerResponseFilter {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
      //TODO: nobody knows why we need this filter. HH-64416 added "!= Response.Status.NO_CONTENT@"
      //if it will be ok, we should remove this filter at all. do it ;-)
      if (response.getEntity() == null && response.getStatusType() != Response.Status.NO_CONTENT) {
        response.setEntity(" ".getBytes());
        response.getHttpHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "text/plain");
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
