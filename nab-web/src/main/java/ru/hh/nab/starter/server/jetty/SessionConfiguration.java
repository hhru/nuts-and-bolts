package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

public class SessionConfiguration extends AbstractConfiguration {

  private final boolean sessionEnabled;

  public SessionConfiguration(boolean sessionEnabled) {
    this.sessionEnabled = sessionEnabled;
  }

  @Override
  public void configure(WebAppContext context) {
    if (!sessionEnabled) {
      context.setSessionHandler(new NoopSessionHandler());
    }
  }
}
