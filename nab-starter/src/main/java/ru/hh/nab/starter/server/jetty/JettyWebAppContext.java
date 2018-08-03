package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import ru.hh.nab.starter.servlet.JerseyServletContextInitializer;

import java.util.ArrayList;
import java.util.List;

final class JettyWebAppContext extends WebAppContext {

  JettyWebAppContext(JerseyServletContextInitializer servletContextInitializer, boolean sessionEnabled) {
    super(null, null, null, null, null, null,
        sessionEnabled ? SESSIONS: 0);

    List<Configuration> configurations = new ArrayList<>();
    configurations.add(new ServletContextInitializerConfiguration(servletContextInitializer));
    setConfigurations(configurations.toArray(new Configuration[0]));
  }

  private static final class ServletContextInitializerConfiguration extends AbstractConfiguration {
    private final JerseyServletContextInitializer servletContextInitializer;

    ServletContextInitializerConfiguration(JerseyServletContextInitializer servletContextInitializer) {
      this.servletContextInitializer = servletContextInitializer;
    }

    @Override
    public void configure(WebAppContext context) {
      context.addBean(new InitializingLifeCycle(context), true);
    }

    final class InitializingLifeCycle extends AbstractLifeCycle {
      private final WebAppContext context;

      InitializingLifeCycle(WebAppContext context) {
        this.context = context;
      }

      @Override
      protected void doStart() {
        servletContextInitializer.onStartup(context);
      }
    }
  }
}
