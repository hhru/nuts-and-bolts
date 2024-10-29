package ru.hh.nab.starter.server.jetty;

import java.util.List;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.starter.servlet.WebAppInitializer;

final class JettyWebAppContext extends WebAppContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebAppContext.class);

  JettyWebAppContext(List<WebAppInitializer> webAppInitializers) {
    super();
    this.addEventListener(new BeforeStartListener(webAppInitializers));
    this.setConfigurations(new Configuration[]{});
    setThrowUnavailableOnStartupException(true);
  }

  private final class BeforeStartListener implements LifeCycle.Listener {
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
