package ru.hh.nab.starter.server.jetty;

import com.timgroup.statsd.NoOpStatsDClient;
import jakarta.annotation.Nullable;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.URIUtil;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import ru.hh.nab.metrics.TaggedSender;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.PORT;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.SESSION_MANAGER_ENABLED;
import ru.hh.nab.starter.servlet.WebAppInitializer;

public final class JettyServerFactory {

  public static JettyServer create(
      FileSettings fileSettings,
      TaggedSender statsDSender,
      List<WebAppInitializer> webAppInitializer
  ) {
    FileSettings jettySettings = fileSettings.getSubSettings(JETTY);
    ServletContextHandler contextHandler = createWebAppContextHandler(jettySettings, webAppInitializer);
    return new JettyServer(jettySettings, statsDSender, contextHandler);
  }

  public static JettyTestServer createTestServer(@Nullable Integer port) {
    try {
      Properties properties = new Properties();
      properties.setProperty(PORT, Optional.ofNullable(port).map(String::valueOf).orElse("0"));
      FileSettings fileSettings = new FileSettings(properties);
      StatsDSender sender = new StatsDSender(new NoOpStatsDClient(), Executors.newScheduledThreadPool(1));
      ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
      TaggedSender appSender = new TaggedSender(sender, Set.of(new Tag(APP_TAG_NAME, "test")));
      JettyServer server = new JettyServer(fileSettings, appSender, handlerCollection);
      server.start();
      return new JettyTestServer(server, handlerCollection);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ServletContextHandler createWebAppContextHandler(FileSettings jettySettings, List<WebAppInitializer> webAppInitializer) {
    boolean sessionEnabled = jettySettings.getBoolean(SESSION_MANAGER_ENABLED, Boolean.FALSE);
    return new JettyWebAppContext(webAppInitializer, sessionEnabled);
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
