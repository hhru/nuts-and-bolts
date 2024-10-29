package ru.hh.nab.starter;

import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.web.context.WebApplicationContext;

public class NabServletContextConfig {

  /**
   * should be called before server doStart() is called
   * Can be used to preconfigure things that required to be inited. Ex: for class-based FilterHolder
   * org.eclipse.jetty.servlet.FilterHolder#initialize() will be called by the container later
   */
  void preConfigureWebApp(WebAppContext webAppContext, WebApplicationContext rootCtx) {
  }
}
