package ru.hh.nab.starter.servlet;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.ApplicationContext;

public interface ServletConfig {

  default String getServletMapping() {
    return "/*";
  }

  void setupServletContext(ServletContextHandler servletContextHandler, ApplicationContext applicationContext);

  default void setupResourceConfig(ResourceConfig resourceConfig) { }
}
