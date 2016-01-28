package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.glassfish.grizzly.http.server.HttpServer;
import ru.hh.nab.grizzly.GrizzlyServerFactory;
import ru.hh.nab.jersey.JerseyHttpServlet;
import javax.inject.Singleton;

class GrizzlyServerModule extends AbstractModule {

  @Override
  protected void configure() { }

  @Provides
  @Singleton
  protected HttpServer jettyServer(Settings settings, JerseyHttpServlet jerseyHttpServlet) {
    return GrizzlyServerFactory.create(
      settings, jerseyHttpServlet);
  }
}
