package ru.hh.nab.starter;

import org.eclipse.jetty.server.Server;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class NabApplicationContext extends AnnotationConfigApplicationContext {

  private volatile Server server;

  boolean isServerRunning() {
    return server.isRunning();
  }

  void setServer(Server server) {
    this.server = server;
  }
}
