package ru.hh.nab.starter.servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class DefaultServletConfig implements ServletConfig {

  @Override
  public void configureServletContext(ServletContextHandler servletContextHandler, ApplicationContext applicationContext) {
    servletContextHandler.addEventListener(new RequestContextListener());
    servletContextHandler.addFilter(RequestIdLoggingFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

    if (applicationContext.containsBean("cacheFilter")) {
      FilterHolder cacheFilter = applicationContext.getBean("cacheFilter", FilterHolder.class);
      if (cacheFilter.isInstance()) {
        servletContextHandler.addFilter(cacheFilter, "/*", EnumSet.allOf(DispatcherType.class));
      }
    }
  }

  @Override
  public void registerResources(ResourceConfig resourceConfig) {
  }
}
