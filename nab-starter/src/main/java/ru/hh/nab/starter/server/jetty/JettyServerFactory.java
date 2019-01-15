package ru.hh.nab.starter.server.jetty;

import java.time.Duration;
import static java.util.Optional.ofNullable;
import static ru.hh.nab.starter.server.jetty.JettyServer.JETTY;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.servlet.WebAppInitializer;

public final class JettyServerFactory {

  private static final int DEFAULT_IDLE_TIMEOUT_MS = (int) Duration.ofMinutes(1).toMillis();

  public static JettyServer create(FileSettings fileSettings, ThreadPool threadPool, WebAppInitializer webAppInitializer) {

    FileSettings jettySettings = fileSettings.getSubSettings(JETTY);
    ServletContextHandler contextHandler = createWebAppContextHandler(jettySettings, webAppInitializer);
    return new JettyServer(threadPool, jettySettings, contextHandler);
  }

  private static ServletContextHandler createWebAppContextHandler(FileSettings jettySettings, WebAppInitializer webAppInitializer) {
    boolean sessionEnabled = ofNullable(jettySettings.getBoolean("session-manager.enabled")).orElse(Boolean.FALSE);
    return new JettyWebAppContext(webAppInitializer, sessionEnabled);
  }

  public static ThreadPool createJettyThreadPool(FileSettings jettySettings) throws Exception {
    int minThreads = ofNullable(jettySettings.getInteger("minThreads")).orElse(4);
    int maxThreads = ofNullable(jettySettings.getInteger("maxThreads")).orElse(12);
    int queueSize = ofNullable(jettySettings.getInteger("queueSize")).orElse(maxThreads);
    int idleTimeoutMs = ofNullable(jettySettings.getInteger("threadPoolIdleTimeoutMs")).orElse(DEFAULT_IDLE_TIMEOUT_MS);
    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeoutMs, new BlockingArrayQueue<>(queueSize));
    threadPool.start();
    return threadPool;
  }

  private JettyServerFactory() {
  }
}
