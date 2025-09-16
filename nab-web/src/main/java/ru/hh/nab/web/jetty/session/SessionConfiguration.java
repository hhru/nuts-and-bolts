package ru.hh.nab.web.jetty.session;

import org.eclipse.jetty.ee10.webapp.AbstractConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;

public class SessionConfiguration extends AbstractConfiguration {

  private final boolean sessionEnabled;

  protected SessionConfiguration(Builder builder, boolean sessionEnabled) {
    super(builder);
    this.sessionEnabled = sessionEnabled;
  }

  @Override
  public void configure(WebAppContext context) {
    if (!sessionEnabled) {
      context.setSessionHandler(new NoopSessionHandler());
    }
  }
}
