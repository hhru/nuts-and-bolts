package ru.hh.nab.testbase;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.server.jetty.JettyServer;

public class JettyTestContainer {
  private final JettyServer jettyServer;
  private final URI baseUri;

  JettyTestContainer(NabApplication application, WebApplicationContext applicationContext, String baseUriString) {
    jettyServer = application.run(applicationContext);
    baseUri = UriBuilder.fromUri(baseUriString).port(jettyServer.getPort()).build();
  }

  public String getBaseUrl() {
    return baseUri.toString();
  }

  public int getPort() {
    return jettyServer.getPort();
  }
}
