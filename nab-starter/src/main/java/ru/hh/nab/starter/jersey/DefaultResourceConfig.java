package ru.hh.nab.starter.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import ru.hh.nab.starter.filters.ResourceNameLoggingFilter;
import ru.hh.nab.starter.resource.StatsResource;
import ru.hh.nab.starter.resource.StatusResource;

public final class DefaultResourceConfig extends ResourceConfig {

  public DefaultResourceConfig() {
    register(FilteredXmlRootElementProvider.App.class);
    register(FilteredXmlRootElementProvider.General.class);
    register(FilteredXmlRootElementProvider.Text.class);
    register(FilteredXmlElementProvider.App.class);
    register(FilteredXmlElementProvider.General.class);
    register(FilteredXmlElementProvider.Text.class);
    register(FilteredXmlListElementProvider.App.class);
    register(FilteredXmlListElementProvider.General.class);
    register(FilteredXmlListElementProvider.Text.class);
    register(ResourceNameLoggingFilter.class);
    register(StatusResource.class);
    register(StatsResource.class);
  }
}
