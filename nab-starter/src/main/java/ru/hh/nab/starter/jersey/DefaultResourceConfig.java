package ru.hh.nab.starter.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import ru.hh.nab.starter.exceptions.AnyExceptionMapper;
import ru.hh.nab.starter.exceptions.IllegalArgumentExceptionMapper;
import ru.hh.nab.starter.exceptions.IllegalStateExceptionMapper;
import ru.hh.nab.starter.exceptions.NotFoundExceptionMapper;
import ru.hh.nab.starter.exceptions.SecurityExceptionMapper;
import ru.hh.nab.starter.exceptions.WebApplicationExceptionMapper;
import ru.hh.nab.starter.filters.ResourceNameLoggingFilter;
import ru.hh.nab.starter.resource.StatsResource;
import ru.hh.nab.starter.resource.StatusResource;

import java.util.Collections;

public final class DefaultResourceConfig extends ResourceConfig {

  public DefaultResourceConfig() {
    addProperties(Collections.singletonMap(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE));

    register(FilteredXmlRootElementProvider.App.class);
    register(FilteredXmlRootElementProvider.General.class);
    register(FilteredXmlRootElementProvider.Text.class);
    register(FilteredXmlElementProvider.App.class);
    register(FilteredXmlElementProvider.General.class);
    register(FilteredXmlElementProvider.Text.class);
    register(FilteredXmlListElementProvider.App.class);
    register(FilteredXmlListElementProvider.General.class);
    register(FilteredXmlListElementProvider.Text.class);

    register(AnyExceptionMapper.class);
    register(IllegalArgumentExceptionMapper.class);
    register(IllegalStateExceptionMapper.class);
    register(NotFoundExceptionMapper.class);
    register(SecurityExceptionMapper.class);
    register(WebApplicationExceptionMapper.class);

    register(ResourceNameLoggingFilter.class);
    register(StatusResource.class);
    register(StatsResource.class);
  }
}
