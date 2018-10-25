package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.webapp.WebAppContext;
import ru.hh.nab.starter.servlet.WebAppInitializer;

final class JettyWebAppContext extends WebAppContext {

  JettyWebAppContext(WebAppInitializer webAppInitializer, boolean sessionEnabled) {
    super(null, null, null, null, null, null,
        sessionEnabled ? SESSIONS: 0);
    webAppInitializer.configureWebApp(this);
  }

  @Override
  protected void loadConfigurations() throws Exception {

  }
}
