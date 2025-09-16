package ru.hh.nab.web.jetty.security;

import org.eclipse.jetty.ee10.webapp.AbstractConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;

public class SecurityConfiguration extends AbstractConfiguration {

  private final boolean securityEnabled;

  protected SecurityConfiguration(Builder builder, boolean securityEnabled) {
    super(builder);
    this.securityEnabled = securityEnabled;
  }

  @Override
  public void configure(WebAppContext context) {
    if (!securityEnabled) {
      context.setSecurityHandler(new NoopSecurityHandler());
    }
  }
}
