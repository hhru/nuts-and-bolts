package ru.hh.nab.grizzly;

import com.google.common.collect.ImmutableMap;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.SimpleDynamicNIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.Settings;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import ru.hh.nab.grizzly.monitoring.ConnectionProbeTimingLogger;

public class SimpleGrizzlyWebServer {
  public static final Map<String, IOStrategy> strategies = ImmutableMap.of(
      "worker", WorkerThreadIOStrategy.getInstance(),
      "same", SameThreadIOStrategy.getInstance(),
      "dynamic", SimpleDynamicNIOStrategy.getInstance(),
      "leader-follower", LeaderFollowerNIOStrategy.getInstance()
  );


  private final NetworkListener grizzlyListener;
  private final HttpServer httpServer;
  private final SimpleGrizzlyAdapterChain adapterChains;
  private final Settings settings;

  private boolean isStarted = false;

  public static SimpleGrizzlyWebServer create(Settings settings, TimingsLoggerFactory timingsLoggerFactory, ConnectionProbeTimingLogger probe) {
    SimpleGrizzlyWebServer s = new SimpleGrizzlyWebServer(settings, timingsLoggerFactory, probe);
    s.configure();
    return s;
  }
  
  private SimpleGrizzlyWebServer(Settings settings, TimingsLoggerFactory timingsLoggerFactory, ConnectionProbeTimingLogger probe) {
    this.settings = settings;
    httpServer = new HttpServer();
    grizzlyListener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, settings.port);
    httpServer.addListener(grizzlyListener);

    this.adapterChains = new SimpleGrizzlyAdapterChain(timingsLoggerFactory, probe);
    getNetworkListener().getTransport().getConnectionMonitoringConfig().addProbes(probe);
    
    addGrizzlyAdapter(new DefaultCharacterEncodingHandler());
  }

  private void configure() {
    setMaxThreads(settings.concurrencyLevel);
    setCoreThreads(settings.concurrencyLevel);
    setWorkerThreadQueueLimit(settings.workersQueueLimit);
    setJmxEnabled(Boolean.valueOf(settings.subTree("grizzly.httpServer").getProperty("jmxEnabled", "false")));
    initNetworkListener(settings.subTree("selector"), settings.subTree("grizzly.memoryManager"));
  }
  
  private void initNetworkListener(Properties selectorProperties, Properties memoryManagerProps) {
    final NetworkListener networkListener = getNetworkListener();
    networkListener.getKeepAlive().setMaxRequestsCount(
        Integer.parseInt(selectorProperties.getProperty("maxKeepAliveRequests", "4096")));
    networkListener.getCompressionConfig().setCompressionMinSize(Integer.MAX_VALUE);
    
    networkListener.setMaxPendingBytes(Integer.parseInt(selectorProperties.getProperty("sendBufferSize", "32768")));
    networkListener.setMaxBufferedPostSize(Integer.parseInt(selectorProperties.getProperty("bufferSize", "32768")));
    networkListener.setMaxHttpHeaderSize(Integer.parseInt(selectorProperties.getProperty("headerSize", "16384")));

    TCPNIOTransport transport = networkListener.getTransport();

    final MemoryManager memoryManager = MemoryManagerFactory.create(memoryManagerProps);
    transport.setMemoryManager(memoryManager);

    boolean blockOnQueueOverflow = Boolean.valueOf(selectorProperties.getProperty("blockOnQueueOverflow", "false"));
    if (blockOnQueueOverflow) {
      transport.setWorkerThreadPool(new BlockedQueueLimitedThreadPool(transport.getWorkerThreadPoolConfig()));
    }

    int ssbacklog = Integer.parseInt(selectorProperties.getProperty("connectionBacklog", "-1"));
    if (blockOnQueueOverflow && ssbacklog < 0) {
      throw new IllegalStateException("Set selector.connectionBacklog size and net.ipv4.tcp_abort_on_overflow=1");
    } else if (ssbacklog > 0) {
      transport.setServerConnectionBackLog(ssbacklog);
    }

    int runnersCount = Integer.parseInt(selectorProperties.getProperty("runnersCount", "-1"));
    if (runnersCount > 0) {
      transport.setSelectorRunnersCount(runnersCount);
    }

    IOStrategy strategy = strategies.get(settings.subTree("grizzly").getProperty("ioStrategy"));
    if (strategy != null) {
      networkListener.getTransport().setIOStrategy(strategy);
    }
    networkListener.getTransport().setTcpNoDelay(true);
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
    httpServer.getServerConfiguration().setDefaultQueryEncoding(Charset.defaultCharset());
    httpServer.start();
  }

  public void setCoreThreads(int coreThreads) {
    grizzlyListener.getTransport().getWorkerThreadPoolConfig().setCorePoolSize(coreThreads);
  }

  public void setMaxThreads(int maxThreads) {
    grizzlyListener.getTransport().getWorkerThreadPoolConfig().setMaxPoolSize(maxThreads);
  }

  public void setWorkerThreadQueueLimit(int limit) {
    grizzlyListener.getTransport().getWorkerThreadPoolConfig().setQueueLimit(limit);
  }

  public void setJmxEnabled(boolean enableJmx) {
    httpServer.getServerConfiguration().setJmxEnabled(enableJmx);
  }

  public synchronized void stop() {
    if (!isStarted) {
      return;
    }
    isStarted = false;
    httpServer.shutdown(1, TimeUnit.MINUTES);
  }
}
