package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.jetty.JettyServerFactory;
import ru.hh.nab.scopes.RequestScopeFilter;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import java.util.EnumSet;

class JettyServerModule extends AbstractModule {

  @Override
  protected void configure() { }

  @Provides
  @Singleton
  protected Server jettyServer(Settings settings, JerseyHttpServlet jerseyHttpServlet) {
    final Server server = JettyServerFactory.create(settings, jerseyHttpServlet);
    WebAppContext context = new WebAppContext();
    context.setContextPath(JerseyHttpServlet.BASE_PATH);
    context.setResourceBase("/bad/local/system/path");
    context.addFilter(RequestScopeFilter.class, JerseyHttpServlet.MAPPING, EnumSet.allOf(DispatcherType.class));
    ServletHolder sh = new ServletHolder();
    sh.setServlet(jerseyHttpServlet);
    sh.setAsyncSupported(true);
    context.addServlet(sh, "/*");
    server.setHandler(context);
    return server;
  }
}
