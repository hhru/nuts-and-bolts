package ru.hh.nab.web.jetty;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import ru.hh.nab.web.servlet.RegistrationValidator;

public class NabJettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  private final ListableBeanFactory beanFactory;

  public NabJettyWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    configureWebAppConfigurations(factory);
  }

  private void configureWebAppConfigurations(JettyServletWebServerFactory factory) {
    factory.addInitializers(new RegistrationValidator(beanFactory));
  }
}
