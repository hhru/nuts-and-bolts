package ru.hh.nab.starter.server.jetty;

import jakarta.annotation.Nullable;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.URIUtil;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.PORT;
import ru.hh.nab.starter.servlet.WebAppInitializer;

public final class JettyServerFactory {

  public static JettyServer create(
      FileSettings fileSettings,
      List<WebAppInitializer> webAppInitializer
  ) {
    FileSettings jettySettings = fileSettings.getSubSettings(JETTY);
    ServletContextHandler contextHandler = createWebAppContextHandler(webAppInitializer);
    return new JettyServer(jettySettings, contextHandler);
  }

  public static JettyTestServer createTestServer(@Nullable Integer port) {
    try {
      Properties properties = new Properties();
      properties.setProperty(PORT, Optional.ofNullable(port).map(String::valueOf).orElse("0"));
      FileSettings fileSettings = new FileSettings(properties);
      ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
      JettyServer server = new JettyServer(fileSettings, handlerCollection);
      server.start();
      return new JettyTestServer(server, handlerCollection);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ServletContextHandler createWebAppContextHandler(List<WebAppInitializer> webAppInitializer) {
    return new JettyWebAppContext(webAppInitializer);
  }

  private JettyServerFactory() {
  }

  public static final class JettyTestServer {
    private final JettyServer jettyServer;
    private final ContextHandlerCollection contextHandlerCollection;
    private final URI baseUri;

    JettyTestServer(JettyServer jettyServer, ContextHandlerCollection contextHandlerCollection) {
      this.jettyServer = jettyServer;
      this.contextHandlerCollection = contextHandlerCollection;
      this.baseUri = getServerAddress(jettyServer.getPort());
    }

    public JettyServer loadServerIfNeeded(ServletContextHandler handler, boolean raiseIfInited) {
      if (contextHandlerCollection.getHandlers() != null && contextHandlerCollection.getHandlers().length > 0) {
        if (raiseIfInited) {
          throw new IllegalStateException("Server already initialized");
        }
        return jettyServer;
      }
      contextHandlerCollection.addHandler(handler);
      try {
        if (!handler.isStarted()) {
          handler.start();
        }
        if (!contextHandlerCollection.isStarted()) {
          contextHandlerCollection.start();
        }
        return jettyServer;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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
}
