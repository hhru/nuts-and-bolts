package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

public class SecurityConfiguration extends AbstractConfiguration {

  @Override
  public void configure(WebAppContext context) {
    context.setSecurityHandler(new NoopSecurityHandler());
  }
}
