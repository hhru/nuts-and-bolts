package ru.hh.nab.starter.server.jetty;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;

import com.timgroup.statsd.NoOpStatsDClient;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.thread.ThreadPool;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.MAX_THREADS;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.MIN_THREADS;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.PORT;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.QUEUE_SIZE;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.SESSION_MANAGER_ENABLED;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.THREAD_POOL_IDLE_TIMEOUT_MS;
import ru.hh.nab.starter.servlet.WebAppInitializer;

import javax.annotation.Nullable;

public final class JettyServerFactory {

  private static final int DEFAULT_IDLE_TIMEOUT_MS = (int) Duration.ofMinutes(1).toMillis();

  public static JettyServer create(FileSettings fileSettings, ThreadPool threadPool, List<WebAppInitializer> webAppInitializer) {
    FileSettings jettySettings = fileSettings.getSubSettings(JETTY);
    ServletContextHandler contextHandler = createWebAppContextHandler(jettySettings, webAppInitializer);
    return new JettyServer(threadPool, jettySettings, contextHandler);
  }

  public static JettyTestServer createTestServer(@Nullable Integer port) {
    try {
      Properties properties = new Properties();
      properties.setProperty(PORT, Optional.ofNullable(port).map(String::valueOf).orElse("0"));
      FileSettings fileSettings = new FileSettings(properties);
      StatsDSender sender = new StatsDSender(new NoOpStatsDClient(), Executors.newScheduledThreadPool(1));
      ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
      JettyServer server = new JettyServer(createJettyThreadPool(fileSettings, "test", sender), fileSettings, handlerCollection);
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

  public static MonitoredQueuedThreadPool createJettyThreadPool(FileSettings jettySettings,
                                                                String serviceName, StatsDSender statsDSender) throws Exception {
    int maxThreads = jettySettings.getInteger(MAX_THREADS, 12);
    int minThreads = jettySettings.getInteger(MIN_THREADS, maxThreads);
    int queueSize = jettySettings.getInteger(QUEUE_SIZE, maxThreads);
    int idleTimeoutMs = jettySettings.getInteger(THREAD_POOL_IDLE_TIMEOUT_MS, DEFAULT_IDLE_TIMEOUT_MS);

    MonitoredQueuedThreadPool threadPool = new MonitoredQueuedThreadPool(
      maxThreads, minThreads, idleTimeoutMs, new BlockingArrayQueue<>(queueSize), serviceName, statsDSender
    );
    threadPool.start();
    return threadPool;
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
