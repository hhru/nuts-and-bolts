package ru.hh.nab.jersey;

import com.google.common.collect.Lists;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import java.util.List;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.SecurityFilter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class JerseyResourceFilterFactory implements ResourceFilterFactory {
  private final PermissionLoader permissionLoader;
  private final List<ResourceFilterFactory> preFilterFactories;
  private final List<ResourceFilterFactory> postFilterFactories;

  @Inject
  public JerseyResourceFilterFactory(
    PermissionLoader permissionLoader,
    @Named("JerseyPreFilterFactories") List<ResourceFilterFactory> preFilterFactories,
    @Named("JerseyPostFilterFactories") List<ResourceFilterFactory> postFilterFactories
  ) {
    this.permissionLoader = permissionLoader;
    this.preFilterFactories = preFilterFactories;
    this.postFilterFactories = postFilterFactories;
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    List<ResourceFilter> filters = Lists.newArrayList();
    preFilterFactories.forEach(ff -> filters.addAll(ff.create(am)));
    addCommonFilters(filters, am);
    postFilterFactories.forEach(ff -> filters.addAll(ff.create(am)));
    return filters;
  }

  private void addCommonFilters(List<ResourceFilter> filters, AbstractMethod am) {
    filters.add(new RequestUrlFilter(am));
    filters.add(new FreemarkerModelFilter());
    if (am.isAnnotationPresent(Cached.class)) {
      filters.add(new CacheControlFilter(am.getAnnotation(Cached.class)));
    }
    filters.add(new SecurityFilter(permissionLoader));
  }
}
