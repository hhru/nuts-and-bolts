package ru.hh.nab.starter;

import jakarta.servlet.ServletContext;
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

  /**
   * should be called after server doStart() is called
   * all static init() methods already called here. So for class-based FilterHolder
   * org.eclipse.jetty.servlet.FilterHolder#initialize() won't be called anymore
   */
  void onWebAppStarted(ServletContext servletContext, WebApplicationContext rootCtx) {
    configureServletContext(servletContext, rootCtx);
  }

  protected void configureServletContext(ServletContext servletContext, WebApplicationContext rootCtx) {
  }

}
