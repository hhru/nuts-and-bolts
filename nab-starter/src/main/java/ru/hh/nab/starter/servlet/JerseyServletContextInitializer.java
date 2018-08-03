package ru.hh.nab.starter.servlet;

import org.eclipse.jetty.servlet.ServletContextHandler;

@FunctionalInterface
public interface JerseyServletContextInitializer {

  void onStartup(ServletContextHandler contextHandler);
}
