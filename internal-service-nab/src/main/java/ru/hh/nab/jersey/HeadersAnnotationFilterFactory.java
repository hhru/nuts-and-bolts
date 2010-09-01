package ru.hh.nab.jersey;

import com.google.inject.Singleton;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class HeadersAnnotationFilterFactory implements ResourceFilterFactory {
  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    List<ResourceFilter> filters = new ArrayList<ResourceFilter>();
    if (am.isAnnotationPresent(Cached.class)) {
      filters.add(new CacheResourceFilter(am.getAnnotation(Cached.class)));
    }
    filters.add(new NginxQuirksFilter());
    return filters;
  }

  private static class CacheResourceFilter implements ResourceFilter {
    private final Cached ann;

    public CacheResourceFilter(Cached ann) {
      this.ann = ann;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
      return null;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
      return new CacheResponseFilter(ann);
    }
  }
}
