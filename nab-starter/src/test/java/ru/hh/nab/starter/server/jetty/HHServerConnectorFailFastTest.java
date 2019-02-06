package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.createServer;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.getPort;

public class HHServerConnectorFailFastTest {

  private static final int acceptors = 1;
  private static final int selectors = 1;
  private static final int workers = 10;
  private static final int threads = acceptors + selectors + workers;

  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  private static final SimpleAsyncHTTPClient httpClient = new SimpleAsyncHTTPClient(executorService);

  private ThreadPool threadPool;
  private ControlledServlet controlledServlet;
  private Server server;

  @Before
  public void beforeTest() {
    threadPool = new QueuedThreadPool(threads, threads);
    controlledServlet = new ControlledServlet(204);
    server = createServer(threadPool, controlledServlet);
  }

  @After
  public void afterTest() throws Exception {
    server.stop();
  }

  @AfterClass
  public static void afterClass() {
    executorService.shutdown();
  }

  @Test
  public void testOriginalServerConnectorAcceptsConnectionsEvenIfLowOnThreads() throws Exception {
    server.addConnector(new ServerConnector(server, acceptors, selectors));
    server.start();
    int serverPort = getPort(server);

    int requests = threads * 2;
    List<Socket> sockets = new ArrayList<>(requests);
    List<Future<Integer>> statusesFutures = new ArrayList<>(requests);

    for(int i=0; i<requests; i++) {
      Socket socket = new Socket("localhost", serverPort);
      sockets.add(socket);
      Future<Integer> statusFuture = httpClient.request(socket);
      statusesFutures.add(statusFuture);
    }

    for(int i=0; i<100; i++) {
      if (!threadPool.isLowOnThreads()) {
        Thread.sleep(5);
      }
    }
    Thread.sleep(5);
    assertTrue(threadPool.isLowOnThreads());

    controlledServlet.respond();

    for (int i=0; i<statusesFutures.size(); i++) {
      assertEquals(204, statusesFutures.get(i).get().intValue());
      sockets.get(i).close();
    }
  }

  @Ignore("HH-88210")
  @Test
  public void testHHServerConnectorResetsNewIncomingConnectionIfLowOnThreads() throws Exception {
    server.addConnector(new HHServerConnector(server, acceptors, selectors));
    server.start();
    int serverPort = getPort(server);

    int requests = threads * 2;
    List<Socket> sockets = new ArrayList<>(requests);
    List<Future<Integer>> statusesFutures = new ArrayList<>(requests);

    for(int i=0; i<requests; i++) {
      Socket socket = new Socket("localhost", serverPort);
      sockets.add(socket);
      Future<Integer> statusFuture = httpClient.request(socket);
      statusesFutures.add(statusFuture);
    }

    for(int i=0; i<100; i++) {
      if (!threadPool.isLowOnThreads()) {
        Thread.sleep(5);
      }
    }
    Thread.sleep(5);

    controlledServlet.respond();

    int successes = 0;
    int failures = 0;
    for (int i=0; i<statusesFutures.size(); i++) {
      Future<Integer> statusFuture = statusesFutures.get(i);
      int status;
      try {
        status = statusFuture.get();
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        assertTrue("Unexpected exception " + cause, cause instanceof SocketException || cause instanceof EOFException);
        failures++;
        continue;
      }
      assertEquals(204, status);
      sockets.get(i).close();
      successes++;
    }
    assertTrue(successes > 0);
    assertTrue(failures > 0);
  }

  static class ControlledServlet extends GenericServlet {

    private final int responseCode;

    private final CountDownLatch proceedLatch = new CountDownLatch(1);

    ControlledServlet(int responseCode) {
      this.responseCode = responseCode;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) {
      try {
        proceedLatch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }

      ((HttpServletResponse) res).setStatus(responseCode);
    }


    void respond() {
      proceedLatch.countDown();
    }
  }
}
