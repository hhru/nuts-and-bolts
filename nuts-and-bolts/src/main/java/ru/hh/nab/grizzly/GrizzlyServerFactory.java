package ru.hh.nab.grizzly;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.Holders;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.SimpleDynamicNIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.Settings;
import ru.hh.nab.Settings.BoolProperty;
import ru.hh.nab.Settings.IntProperty;
import ru.hh.nab.grizzly.monitoring.ConnectionProbeTimingLogger;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.scopes.RequestScope.RequestContext;
import ru.hh.nab.scopes.RequestScopeFilter;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;

public final class GrizzlyServerFactory {
  public static final Map<String, IOStrategy> strategies = ImmutableMap.of(
      "worker", WorkerThreadIOStrategy.getInstance(),
      "same", SameThreadIOStrategy.getInstance(),
      "dynamic", SimpleDynamicNIOStrategy.getInstance(),
      "leader-follower", LeaderFollowerNIOStrategy.getInstance()
  );

  static final IntProperty SELECTOR_MAX_KEEP_ALIVE_REQUESTS = new IntProperty("maxKeepAliveRequests", 4096);
  static final IntProperty SELECTOR_MAX_PENDING_BYTES = new IntProperty("sendBufferSize", 32768);
  static final IntProperty SELECTOR_MAX_BUFFERED_POST_SIZE = new IntProperty("bufferSize", 32768);
  static final IntProperty SELECTOR_MAX_HTTP_HEADER_SIZE = new IntProperty("headerSize", 16384);
  static final IntProperty SELECTOR_CONNECTION_BACKLOG = new IntProperty("connectionBacklog", -1);
  static final IntProperty SELECTOR_RUNNERS_COUNT = new IntProperty("runnersCount", -1);
  static final BoolProperty SELECTOR_BLOCK_ON_QUEUE_OVERFLOW = new BoolProperty("blockOnQueueOverflow", false);

  public static HttpServer create(Settings settings, JerseyHttpServlet jerseyHttpServlet) {
    final ConnectionProbeTimingLogger probe = new ConnectionProbeTimingLogger(LoggerFactory.getLogger(TimingsLogger.class));
    final HttpServer server = new HttpServer();
    setJmxEnabled(server, Boolean.valueOf(settings.subTree("grizzly.httpServer").getProperty("jmxEnabled", "false")));
    configureNetworking(settings, server, probe);
    server.getServerConfiguration().setDefaultQueryEncoding(Charset.defaultCharset());
    configureJerseyServlet(server, jerseyHttpServlet, probe);
    return server;
  }

  private static void configureNetworking(Settings settings, HttpServer server, ConnectionProbeTimingLogger probe) {
    final Properties selectorProperties = settings.subTree("selector");
    final Properties memoryManagerProps = settings.subTree("grizzly.memoryManager");

    final NetworkListener networkListener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, settings.port);
    server.addListener(networkListener);
    networkListener.getKeepAlive().setMaxRequestsCount(SELECTOR_MAX_KEEP_ALIVE_REQUESTS.from(selectorProperties));
    networkListener.getCompressionConfig().setCompressionMinSize(Integer.MAX_VALUE);
    networkListener.setMaxPendingBytes(SELECTOR_MAX_PENDING_BYTES.from(selectorProperties));
    networkListener.setMaxBufferedPostSize(SELECTOR_MAX_BUFFERED_POST_SIZE.from(selectorProperties));
    networkListener.setMaxHttpHeaderSize(SELECTOR_MAX_HTTP_HEADER_SIZE.from(selectorProperties));

    final TCPNIOTransport transport = networkListener.getTransport();
    final MemoryManager memoryManager = MemoryManagerFactory.create(memoryManagerProps);
    transport.setMemoryManager(memoryManager);

    final boolean blockOnQueueOverflow = SELECTOR_BLOCK_ON_QUEUE_OVERFLOW.getFrom(selectorProperties);

    // Configure thread pool and queue
    final ThreadPoolConfig threadPoolConfig = transport.getWorkerThreadPoolConfig();
    threadPoolConfig.setMaxPoolSize(settings.concurrencyLevel);
    threadPoolConfig.setCorePoolSize(settings.concurrencyLevel);
    threadPoolConfig.setQueueLimit(settings.workersQueueLimit);
    if (blockOnQueueOverflow) {
      transport.setWorkerThreadPool(new BlockedQueueLimitedThreadPool(threadPoolConfig));
    }

    final int ssbacklog = SELECTOR_CONNECTION_BACKLOG.from(selectorProperties);
    if (blockOnQueueOverflow && ssbacklog < 0) {
      throw new IllegalStateException("Set selector.connectionBacklog size and net.ipv4.tcp_abort_on_overflow=1");
    } else if (ssbacklog > 0) {
      transport.setServerConnectionBackLog(ssbacklog);
    }

    final int runnersCount = SELECTOR_RUNNERS_COUNT.from(selectorProperties);
    if (runnersCount > 0) {
      transport.setSelectorRunnersCount(runnersCount);
    }

    IOStrategy strategy = strategies.get(settings.subTree("grizzly").getProperty("ioStrategy"));
    if (strategy != null) {
      transport.setIOStrategy(strategy);
    }
    transport.setTcpNoDelay(true);

    transport.getConnectionMonitoringConfig().addProbes(probe);
  }

  private static void configureJerseyServlet(HttpServer server, JerseyHttpServlet jerseyHttpServlet, ConnectionProbeTimingLogger probe) {
    WebappContext webappContext = new WebappContext("GrizzlyWebApp");
    FilterRegistration requestScopeFilterRegistration = webappContext.addFilter("RequestScopeFilter", RequestScopeFilter.class);
    requestScopeFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), JerseyHttpServlet.MAPPING);
    requestScopeFilterRegistration.setAsyncSupported(true);

    FilterRegistration connectionProbeFilterRegistration = webappContext.addFilter("ConnectionProbeFilter",
      new Filter() {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
          final String requestId = ((HttpServletRequest) request).getHeader(RequestContext.X_REQUEST_ID);
          final Connection connection = ((Holders.RequestHolder) request).getInternalRequest().getRequest().getConnection();
          chain.doFilter(request, response);
          if (StringUtils.isBlank(requestId)) {
            return;
          }

          if (!request.isAsyncStarted()) {
            probe.endUserRequest(requestId, connection);
          } else {
            request.getAsyncContext().addListener(new AsyncListener() {
              @Override
              public void onComplete(AsyncEvent event) throws IOException {
                probe.endUserRequest(requestId, connection);
              }

              @Override
              public void onTimeout(AsyncEvent event) throws IOException {
                probe.endUserRequest(requestId, connection);
              }

              @Override
              public void onError(AsyncEvent event) throws IOException {
                probe.endUserRequest(requestId, connection);
              }

              @Override
              public void onStartAsync(AsyncEvent event) throws IOException {
              }
            });
          }
        }

        @Override
        public void destroy() {
        }
      }
    );
    connectionProbeFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), JerseyHttpServlet.MAPPING);
    connectionProbeFilterRegistration.setAsyncSupported(true);

    ServletRegistration servletRegistration = webappContext.addServlet("JerseyHttpServlet", jerseyHttpServlet);
    servletRegistration.addMapping(JerseyHttpServlet.MAPPING);
    servletRegistration.setAsyncSupported(true);

    webappContext.deploy(server);
  }

  private static void setJmxEnabled(HttpServer server, boolean enableJmx) {
    server.getServerConfiguration().setJmxEnabled(enableJmx);
  }
}
