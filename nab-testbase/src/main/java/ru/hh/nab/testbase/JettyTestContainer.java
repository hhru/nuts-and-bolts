package ru.hh.nab.testbase;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.testbase.spring.NabTestContext;
import static org.eclipse.jetty.util.URIUtil.HTTP;

public class JettyTestContainer {
  private final JettyServer jettyServer;
  private final URI baseUri;

  JettyTestContainer(NabApplication application, WebApplicationContext applicationContext, NabTestContext.PortHolder portHolder) {
    jettyServer = application.run(applicationContext, false, portHolder::releaseAndApply, false);
    baseUri = getServerAddress(jettyServer.getPort());
  }

  public static URI getServerAddress(int port) {
    try {
      return new URI(HTTP, null, InetAddress.getLoopbackAddress().getHostAddress(), port, null, null, null);
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
