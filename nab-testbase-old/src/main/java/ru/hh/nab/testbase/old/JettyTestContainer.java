package ru.hh.nab.testbase.old;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jetty.util.URIUtil;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.testbase.old.spring.NabTestContext;

public class JettyTestContainer {
  private final JettyServer jettyServer;
  private final URI baseUri;

  JettyTestContainer(NabApplication application, WebApplicationContext applicationContext, NabTestContext.PortHolder portHolder) {
    JettyServerFactory.JettyTestServer testServer = portHolder.releaseAndApply(JettyServerFactory::createTestServer);
    jettyServer = application.runOnTestServer(testServer, applicationContext, false);
    baseUri = getServerAddress(jettyServer.getPort());
  }

  public static URI getServerAddress(int port) {
    try {
      return new URI(URIUtil.HTTP, null, InetAddress.getLoopbackAddress().getHostAddress(), port, null, null, null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public String getBaseUrl() {
    return baseUri.toString();
  }

  public int getPort() {
    return jettyServer.getPort();
  }
}
