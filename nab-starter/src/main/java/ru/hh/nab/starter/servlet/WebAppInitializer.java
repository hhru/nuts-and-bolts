package ru.hh.nab.starter.servlet;

import org.eclipse.jetty.webapp.WebAppContext;

@FunctionalInterface
public interface WebAppInitializer {

  void configureWebApp(WebAppContext app);
}
