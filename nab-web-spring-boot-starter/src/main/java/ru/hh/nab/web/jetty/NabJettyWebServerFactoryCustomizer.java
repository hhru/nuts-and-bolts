package ru.hh.nab.web.jetty;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.webapp.Configuration;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.annotation.Order;
import ru.hh.nab.starter.server.jetty.SecurityConfiguration;
import ru.hh.nab.starter.server.jetty.SessionConfiguration;
import ru.hh.nab.starter.server.jetty.ThreadPoolProxyFactory;
import ru.hh.nab.web.ExtendedServerProperties;

@Order
public class NabJettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  private final ExtendedServerProperties extendedServerProperties;
  private final ThreadPoolProxyFactory threadPoolProxyFactory;
  private final List<Configuration> webAppConfigurations;

  public NabJettyWebServerFactoryCustomizer(
      ExtendedServerProperties extendedServerProperties,
      ThreadPoolProxyFactory threadPoolProxyFactory,
      List<Configuration> webAppConfigurations
  ) {
    this.extendedServerProperties = extendedServerProperties;
    this.threadPoolProxyFactory = threadPoolProxyFactory;
    this.webAppConfigurations = webAppConfigurations;
  }

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    factory.setThreadPool(threadPoolProxyFactory.create(factory.getThreadPool()));
    factory.addConfigurations(getWebAppConfigurations().toArray(Configuration[]::new));
  }

  private List<Configuration> getWebAppConfigurations() {
    List<Configuration> webAppConfigurations = new ArrayList<>();
    webAppConfigurations.add(
        new SessionConfiguration(extendedServerProperties.getServlet().getSession().isEnabled())
    );
    webAppConfigurations.add(new SecurityConfiguration());
    webAppConfigurations.addAll(this.webAppConfigurations);
    return webAppConfigurations;
  }
}
