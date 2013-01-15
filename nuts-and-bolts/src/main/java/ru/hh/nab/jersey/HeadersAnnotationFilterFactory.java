package ru.hh.nab.jersey;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import java.util.List;
import javax.ws.rs.ext.Provider;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.SecurityFilter;

@Provider
@Singleton
public class HeadersAnnotationFilterFactory implements ResourceFilterFactory {
  private PermissionLoader permissionLoader;

  @Inject
  public HeadersAnnotationFilterFactory(PermissionLoader permissionLoader) {
    this.permissionLoader = permissionLoader;
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    List<ResourceFilter> filters = Lists.newArrayList();
    filters.add(new FreemarkerModelFilter());
    if (am.isAnnotationPresent(Cached.class)) {
      filters.add(new CacheControlFilter(am.getAnnotation(Cached.class)));
    }
    filters.add(new NginxQuirksFilter());
    filters.add(new SecurityFilter(permissionLoader));
    return filters;
  }
}
