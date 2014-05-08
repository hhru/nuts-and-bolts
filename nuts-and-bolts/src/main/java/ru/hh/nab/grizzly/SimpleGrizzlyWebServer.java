package ru.hh.nab.grizzly;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SimpleGrizzlyWebServer {
  private final NetworkListener grizzlyListener;
  private final HttpServer httpServer;
  private final SimpleGrizzlyAdapterChain adapterChains;

  private boolean isStarted = false;

  public SimpleGrizzlyWebServer(int port, int maxThreads, TimingsLoggerFactory timingsLoggerFactory) {
    httpServer = new HttpServer();
    grizzlyListener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, port);
    httpServer.addListener(grizzlyListener);
    setMaxThreads(maxThreads);
    this.adapterChains = new SimpleGrizzlyAdapterChain(timingsLoggerFactory);
  }

  public NetworkListener getNetworkListener() {
    return grizzlyListener;
  }

  public void addGrizzlyAdapter(HttpHandler grizzlyAdapter) {
    adapterChains.addGrizzlyAdapter(grizzlyAdapter);
  }

  public synchronized void start() throws IOException {
    if (isStarted) {
      return;
    }
    isStarted = true;

    httpServer.getServerConfiguration().addHttpHandler(adapterChains);
    httpServer.start();
  }

  public void setCoreThreads(int coreThreads) {
    grizzlyListener.getTransport().getWorkerThreadPoolConfig().setCorePoolSize(coreThreads);
  }

  public void setMaxThreads(int maxThreads) {
    grizzlyListener.getTransport().getWorkerThreadPoolConfig().setMaxPoolSize(maxThreads);
  }

  public synchronized void stop() {
    if (!isStarted) return;
    isStarted = false;
    httpServer.shutdown(1, TimeUnit.MINUTES);
  }
}
