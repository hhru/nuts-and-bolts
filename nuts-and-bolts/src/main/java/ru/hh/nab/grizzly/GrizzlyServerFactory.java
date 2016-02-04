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
import ru.hh.nab.grizzly.monitoring.ConnectionProbeTimingLogger;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;

import static ru.hh.nab.Settings.getBoolProperty;
import static ru.hh.nab.Settings.getIntProperty;

public final class GrizzlyServerFactory {
  public static final Map<String, IOStrategy> strategies = ImmutableMap.of(
      "worker", WorkerThreadIOStrategy.getInstance(),
      "same", SameThreadIOStrategy.getInstance(),
      "dynamic", SimpleDynamicNIOStrategy.getInstance(),
      "leader-follower", LeaderFollowerNIOStrategy.getInstance()
  );

  public static HttpServer create(Settings settings, HttpServlet httpServlet) {
    final ConnectionProbeTimingLogger probe = new ConnectionProbeTimingLogger(LoggerFactory.getLogger(TimingsLogger.class));
    final HttpServer server = new HttpServer();
    setJmxEnabled(server, Boolean.valueOf(settings.subTree("grizzly.httpServer").getProperty("jmxEnabled", "false")));
    configureNetworking(settings, server, probe);
    server.getServerConfiguration().setDefaultQueryEncoding(Charset.defaultCharset());
    createWebapp(httpServlet, probe).deploy(server);
    return server;
  }

  private static void configureNetworking(Settings settings, HttpServer server, ConnectionProbeTimingLogger probe) {
    final Properties selectorProps = settings.subTree("selector");

    final NetworkListener networkListener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, settings.port);
    server.addListener(networkListener);
    networkListener.getKeepAlive().setMaxRequestsCount(getIntProperty(selectorProps, "maxKeepAliveRequests", 4096));
    networkListener.getCompressionConfig().setCompressionMinSize(Integer.MAX_VALUE);
    networkListener.setMaxPendingBytes(getIntProperty(selectorProps, "sendBufferSize", 32768));
    networkListener.setMaxBufferedPostSize(getIntProperty(selectorProps, "bufferSize", 32768));
    networkListener.setMaxHttpHeaderSize(getIntProperty(selectorProps, "headerSize", 16384));

    final TCPNIOTransport transport = networkListener.getTransport();
    final MemoryManager memoryManager = MemoryManagerFactory.create(settings.subTree("grizzly.memoryManager"));
    transport.setMemoryManager(memoryManager);

    final boolean blockOnQueueOverflow = getBoolProperty(selectorProps, "blockOnQueueOverflow", false);

    // Configure thread pool and queue
    final ThreadPoolConfig threadPoolConfig = transport.getWorkerThreadPoolConfig();
    final Properties grizzlyProps = settings.subTree("grizzly");
    final int concurrencyLevel = getIntProperty(grizzlyProps, "concurrencyLevel", -1);
    if (concurrencyLevel <= 0) {
      throw new IllegalArgumentException("grizzly.concurrencyLevel is required and must be > 0");
    }
    final int workersQueueLimit = getIntProperty(grizzlyProps, "workersQueueLimit", -1);
    threadPoolConfig.setMaxPoolSize(concurrencyLevel);
    threadPoolConfig.setCorePoolSize(concurrencyLevel);
    threadPoolConfig.setQueueLimit(workersQueueLimit);
    if (blockOnQueueOverflow) {
      transport.setWorkerThreadPool(new BlockedQueueLimitedThreadPool(threadPoolConfig));
    }

    final int ssbacklog = getIntProperty(selectorProps, "connectionBacklog", -1);
    if (blockOnQueueOverflow && ssbacklog < 0) {
      throw new IllegalStateException("Set selector.connectionBacklog size and net.ipv4.tcp_abort_on_overflow=1");
    } else if (ssbacklog > 0) {
      transport.setServerConnectionBackLog(ssbacklog);
    }

    final int runnersCount = getIntProperty(selectorProps, "runnersCount", -1);
    if (runnersCount > 0) {
      transport.setSelectorRunnersCount(runnersCount);
    }

    IOStrategy strategy = strategies.get(grizzlyProps.getProperty("ioStrategy"));
    if (strategy != null) {
      transport.setIOStrategy(strategy);
    }
    transport.setTcpNoDelay(true);

    transport.getConnectionMonitoringConfig().addProbes(probe);
  }

  private static WebappContext createWebapp(HttpServlet httpServlet, ConnectionProbeTimingLogger probe) {
    WebappContext webappContext = new WebappContext("GrizzlyWebApp");
    FilterRegistration requestScopeFilterRegistration = webappContext.addFilter("RequestScopeFilter", RequestScopeFilter.class);
    requestScopeFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");
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
    connectionProbeFilterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");
    connectionProbeFilterRegistration.setAsyncSupported(true);

    ServletRegistration servletRegistration = webappContext.addServlet("nab-" + httpServlet.getClass().getName(), httpServlet);
    servletRegistration.addMapping("/*");
    servletRegistration.setAsyncSupported(true);
    return webappContext;
  }

  private static void setJmxEnabled(HttpServer server, boolean enableJmx) {
    server.getServerConfiguration().setJmxEnabled(enableJmx);
  }
}
