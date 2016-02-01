package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.eclipse.jetty.server.Server;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.jetty.JettyServerFactory;
import javax.inject.Singleton;

class JettyServerModule extends AbstractModule {

  @Override
  protected void configure() { }

  @Provides
  @Singleton
  protected Server jettyServer(Settings settings, JerseyHttpServlet jerseyHttpServlet) {
    return JettyServerFactory.create(settings, jerseyHttpServlet);
  }
}
