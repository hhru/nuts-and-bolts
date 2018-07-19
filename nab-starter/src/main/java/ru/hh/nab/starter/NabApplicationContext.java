package ru.hh.nab.starter;

import org.eclipse.jetty.server.Server;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class NabApplicationContext extends AnnotationConfigWebApplicationContext {

  private volatile Server server;

  boolean isServerRunning() {
    return server.isRunning();
  }

  void setServer(Server server) {
    this.server = server;
  }
}
