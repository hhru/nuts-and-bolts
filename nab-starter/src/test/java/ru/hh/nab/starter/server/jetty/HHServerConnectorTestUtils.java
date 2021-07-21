package ru.hh.nab.starter.server.jetty;

import javax.servlet.Servlet;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ThreadPool;

class HHServerConnectorTestUtils {

  static void repeat(int times, TestIteration testIteration) throws Exception {
    for (int i=1; i<=times; i++) {
      System.out.println("Running iteration " + i + " of " + times);
      testIteration.run();
    }
  }

  interface TestIteration {
    void run() throws Exception;
  }

  static Server createServer(ThreadPool threadPool, Servlet servlet) {
    ServletHolder servletHolder = new ServletHolder("MainServlet", servlet);

    ServletHandler servletHandler = new ServletHandler();
    servletHandler.addServletWithMapping(servletHolder, "/*");

    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setServletHandler(servletHandler);

    Server server = new Server(threadPool);
    server.setHandler(servletContextHandler);
    server.setStopAtShutdown(true);
    return server;
  }

  static int getPort(Server server) {
    return ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
  }

  private HHServerConnectorTestUtils() {
  }
}
