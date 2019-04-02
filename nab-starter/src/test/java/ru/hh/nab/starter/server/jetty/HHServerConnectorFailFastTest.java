package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.createServer;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.getPort;

public class HHServerConnectorFailFastTest {

  private static final int ACCEPTORS = 1;
  private static final int SELECTORS = 1;
  private static final int WORKERS = 10;
  private static final int THREADS = ACCEPTORS + SELECTORS + WORKERS;
  private static final int REQUESTS = THREADS * 3; // must be > than threads + queue size

  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  private static final SimpleAsyncHTTPClient httpClient = new SimpleAsyncHTTPClient(executorService);

  private ThreadPool threadPool;
  private ControlledServlet controlledServlet;
  private Server server;

  @Before
  public void beforeTest() {
    threadPool = new QueuedThreadPool(THREADS, THREADS);
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
    server.addConnector(new ServerConnector(server, ACCEPTORS, SELECTORS));
    server.start();
    int serverPort = getPort(server);

    List<Socket> sockets = new ArrayList<>(REQUESTS);
    List<Future<Integer>> statusesFutures = new ArrayList<>(REQUESTS);

    for (int i = 0; i < REQUESTS; i++) {
      Socket socket = new Socket("localhost", serverPort);
      sockets.add(socket);
      Future<Integer> statusFuture = httpClient.request(socket);
      statusesFutures.add(statusFuture);
    }

    await().atMost(500, TimeUnit.MILLISECONDS).until(threadPool::isLowOnThreads);

    controlledServlet.respond();

    for (int i = 0; i < statusesFutures.size(); i++) {
      assertEquals(204, statusesFutures.get(i).get().intValue());
      sockets.get(i).close();
    }
  }

  @Test
  public void testHHServerConnectorResetsNewIncomingConnectionIfLowOnThreads() throws Exception {
    server.addConnector(new HHServerConnector(server, ACCEPTORS, SELECTORS));
    server.start();
    int serverPort = getPort(server);

    List<Socket> sockets = new ArrayList<>(REQUESTS);
    List<Future<Integer>> statusesFutures = new ArrayList<>(REQUESTS);

    for (int i=0; i < REQUESTS; i++) {
      Socket socket = new Socket("localhost", serverPort);
      sockets.add(socket);
      Future<Integer> statusFuture = httpClient.request(socket);
      statusesFutures.add(statusFuture);
    }

    await().atMost(500, TimeUnit.MILLISECONDS).until(threadPool::isLowOnThreads);

    controlledServlet.respond();

    int successes = 0;
    int failures = 0;
    for (int i = 0; i < statusesFutures.size(); i++) {
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
