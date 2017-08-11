package ru.hh.nab.jersey;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class JerseyResourceFilterFactory implements ResourceFilterFactory {
  private final List<ResourceFilterFactory> preFilterFactories;
  private final List<ResourceFilterFactory> postFilterFactories;

  @Inject
  public JerseyResourceFilterFactory(
    @Named("JerseyPreFilterFactories") List<ResourceFilterFactory> preFilterFactories,
    @Named("JerseyPostFilterFactories") List<ResourceFilterFactory> postFilterFactories
  ) {
    this.preFilterFactories = preFilterFactories;
    this.postFilterFactories = postFilterFactories;
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    List<ResourceFilter> filters = new ArrayList<>();
    preFilterFactories.forEach(ff -> filters.addAll(ff.create(am)));
    addCommonFilters(filters, am);
    postFilterFactories.forEach(ff -> filters.addAll(ff.create(am)));
    return filters;
  }

  private void addCommonFilters(List<ResourceFilter> filters, AbstractMethod am) {
    filters.add(new RequestUrlFilter(am));
    filters.add(new FreemarkerModelFilter());
  }
}
