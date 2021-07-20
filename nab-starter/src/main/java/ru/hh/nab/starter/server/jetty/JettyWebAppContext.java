package ru.hh.nab.starter.server.jetty;

import java.util.List;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.starter.servlet.WebAppInitializer;

final class JettyWebAppContext extends WebAppContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebAppContext.class);

  JettyWebAppContext(List<WebAppInitializer> webAppInitializers, boolean sessionEnabled) {
    super(null, null, null, null, null, null, sessionEnabled ? SESSIONS: 0);
    this.addLifeCycleListener(new BeforeStartListener(webAppInitializers));
    setThrowUnavailableOnStartupException(true);
  }

  @Override
  protected void loadConfigurations() throws Exception {
    if (getConfigurations().length > 0) {
      return;
    }

    String[] configurationClassStrings = getConfigurationClasses();
    Configuration[] configurations = new Configuration[configurationClassStrings.length];
    for (int i = 0; i < configurations.length; i++) {
      configurations[i] = (Configuration)Loader.loadClass(configurationClassStrings[i]).getDeclaredConstructor().newInstance();
    }
    setConfigurations(configurations);
  }

  private final class BeforeStartListener extends AbstractLifeCycleListener {
    private final List<WebAppInitializer> initializers;

    private BeforeStartListener(List<WebAppInitializer> initializers) {
      this.initializers = initializers;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
      LOGGER.debug("Initializing webApp");
      initializers.forEach(initializer -> initializer.configureWebApp(JettyWebAppContext.this));
    }
  }
}
