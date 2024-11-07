package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

public class SecurityConfiguration extends AbstractConfiguration {

  private final boolean securityEnabled;

  public SecurityConfiguration(boolean securityEnabled) {
    this.securityEnabled = securityEnabled;
  }

  @Override
  public void configure(WebAppContext context) {
    if (!securityEnabled) {
      context.setSecurityHandler(new NoopSecurityHandler());
    }
  }
}
