package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.webapp.Configuration;
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
    if (getConfigurations().length > 0) {
      return;
    }

    String[] configurationClassStrings = getConfigurationClasses();
    Configuration[] configurations = new Configuration[configurationClassStrings.length];
    for (int i = 0; i < configurations.length; i++) {
      configurations[i] = (Configuration)Loader.loadClass(configurationClassStrings[i]).newInstance();
    }
    setConfigurations(configurations);
  }
}
