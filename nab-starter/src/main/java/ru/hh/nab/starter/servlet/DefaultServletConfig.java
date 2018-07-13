package ru.hh.nab.starter.servlet;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;
import ru.hh.nab.starter.filters.ResourceNameLoggingFilter;
import ru.hh.nab.starter.jersey.FilteredXmlElementProvider;
import ru.hh.nab.starter.jersey.FilteredXmlListElementProvider;
import ru.hh.nab.starter.jersey.FilteredXmlRootElementProvider;

public class DefaultServletConfig implements ServletConfig {

  @Override
  public void configureServletContext(ServletContextHandler servletContextHandler, ApplicationContext applicationContext) {
    servletContextHandler.addFilter(RequestIdLoggingFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

    if (applicationContext.containsBean("cacheFilter")) {
      FilterHolder cacheFilter = applicationContext.getBean("cacheFilter", FilterHolder.class);
      if (cacheFilter.isInstance()) {
        servletContextHandler.addFilter(cacheFilter, "/*", EnumSet.allOf(DispatcherType.class));
      }
    }
  }

  @Override
  public ResourceConfig createResourceConfig(ApplicationContext context) {
    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.property("contextConfig", context);
    context.getBeansWithAnnotation(javax.ws.rs.Path.class)
        .forEach((name, resource) -> resourceConfig.register(resource));

    resourceConfig.register(FilteredXmlRootElementProvider.App.class);
    resourceConfig.register(FilteredXmlRootElementProvider.General.class);
    resourceConfig.register(FilteredXmlRootElementProvider.Text.class);
    resourceConfig.register(FilteredXmlElementProvider.App.class);
    resourceConfig.register(FilteredXmlElementProvider.General.class);
    resourceConfig.register(FilteredXmlElementProvider.Text.class);
    resourceConfig.register(FilteredXmlListElementProvider.App.class);
    resourceConfig.register(FilteredXmlListElementProvider.General.class);
    resourceConfig.register(FilteredXmlListElementProvider.Text.class);
    resourceConfig.register(new ResourceNameLoggingFilter());
    return resourceConfig;
  }
}
