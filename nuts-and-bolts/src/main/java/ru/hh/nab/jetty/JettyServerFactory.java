package ru.hh.nab.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import ru.hh.jetty.RequestLogger;
import ru.hh.nab.Settings;
import ru.hh.nab.scopes.RequestScopeFilter;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import java.util.Properties;

import static ru.hh.nab.Settings.getIntProperty;

public abstract class JettyServerFactory {

  public static Server create(Settings settings, HttpServlet httpServlet) {

    final Properties props = settings.subTree("jetty");

    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setOutputBufferSize(getIntProperty(props, "outputBufferSize", 65536));
    httpConfiguration.setRequestHeaderSize(getIntProperty(props, "requestHeaderSize", 16384));
    httpConfiguration.setResponseHeaderSize(getIntProperty(props, "responseHeaderSize", 65536));
    httpConfiguration.setHeaderCacheSize(getIntProperty(props, "headerCacheSize", 512));
    httpConfiguration.setBlockingTimeout(getIntProperty(props, "blockingTimeout", 5000));

    httpConfiguration.setSendServerVersion(false);
    httpConfiguration.setSendDateHeader(true);

    final ThreadPool threadPool = new QueuedThreadPool(
      getIntProperty(props, "maxThreads", 128),
      getIntProperty(props, "minThreads", 8),
      getIntProperty(props, "idleThreadTimeoutMs", 60000)
    );

    final Server server = new Server(threadPool);

    final MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addEventListener(mbContainer);
    server.addBean(mbContainer);
    server.addBean(Log.getLog());

    server.setRequestLog(new RequestLogger());

    final HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);

    final ServerConnector serverConnector = new ServerConnector(
      server,
      getIntProperty(props, "acceptors", 1),
      getIntProperty(props, "selectors", -1),
      httpConnectionFactory
    );
    serverConnector.setPort(settings.port);
    serverConnector.setIdleTimeout(getIntProperty(props, "idleConnectionTimeoutMs", 30000));
    serverConnector.setAcceptQueueSize(getIntProperty(props, "acceptQueueSize", 50));

    server.addConnector(serverConnector);

    server.setStopAtShutdown(true);
    server.setStopTimeout(getIntProperty(props, "serverStopTimeout", 5000));

    server.setHandler(createWebapp(httpServlet));

    return server;
  }

  private static ServletContextHandler createWebapp(HttpServlet httpServlet) {
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.addFilter(RequestScopeFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    ServletHolder sh = new ServletHolder();
    sh.setServlet(httpServlet);
    sh.setAsyncSupported(true);
    context.addServlet(sh, "/*");
    return context;
  }
}
