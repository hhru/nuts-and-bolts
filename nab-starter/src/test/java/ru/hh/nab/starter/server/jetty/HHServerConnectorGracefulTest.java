package ru.hh.nab.starter.server.jetty;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.getPort;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.repeat;

public class HHServerConnectorGracefulTest {

  private static final int STOP_TIMEOUT_MS = 10_000;

  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  private static final SimpleAsyncHTTPClient httpClient = new SimpleAsyncHTTPClient(executorService);

  @AfterAll
  public static void afterGracefulServletContextHandlerTestClass() {
    executorService.shutdown();
  }

  @Test
  public void testOriginalServerConnectorDoesNotWaitPendingRequests() throws Exception {
    ControlledServlet controlledServlet = new ControlledServlet(204);
    Server server = createServer(controlledServlet);
    server.addConnector(new ServerConnector(server));
    server.start();

    Socket requestSocket = new Socket("localhost", getPort(server));
    Future<Integer> responseStatusFuture = httpClient.request(requestSocket);
    controlledServlet.awaitRequest();

    Future<Void> serverStoppedFuture = stop(server);
    Thread.sleep(100);
    controlledServlet.respond();
    serverStoppedFuture.get(STOP_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);

    assertTrue(responseStatusFuture.isDone());
    try {
      responseStatusFuture.get();
      fail("original jetty ServletContextHandler did wait for request to complete");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof EOFException);
    }

    requestSocket.close();
  }

  @Test
  public void testHHServerConnectorWaitsPendingRequests() throws Exception {
    repeat(100, () -> {
      ControlledServlet controlledServlet = new ControlledServlet(204);
      Server server = createServer(controlledServlet);
      server.addConnector(new HHServerConnector(server));
      server.start();
      int serverPort = getPort(server);

      Socket idleSocket = new Socket("localhost", serverPort);

      Socket requestSocket = new Socket("localhost", serverPort);
      Future<Integer> responseStatusFuture = httpClient.request(requestSocket);

      controlledServlet.awaitRequest();

      Future<Void> serverStoppedFuture = stop(server);
      Thread.sleep(100);

      assertEquals(503, httpClient.request(idleSocket).get().intValue());

      try {
        new Socket("localhost", serverPort);
        fail("connection was not refused after server begin to stop");
      } catch (ConnectException e) {
      }

      controlledServlet.respond();

      serverStoppedFuture.get(STOP_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);

      assertEquals(204, responseStatusFuture.get().intValue());

      idleSocket.close();
      requestSocket.close();
    });
  }

  static class ControlledServlet extends GenericServlet {

    private final int responseCode;

    private final BlockingQueue<CountDownLatch> arrivedRequests = new LinkedBlockingQueue<>();
    private final Queue<CountDownLatch> readyToProceedRequests = new ArrayDeque<>();

    ControlledServlet(int responseCode) {
      this.responseCode = responseCode;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) {
      CountDownLatch latch = new CountDownLatch(1);
      arrivedRequests.add(latch);
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
      ((HttpServletResponse) res).setStatus(responseCode);
    }

    ControlledServlet awaitRequest() throws InterruptedException {
      readyToProceedRequests.add(arrivedRequests.take());
      return this;
    }

    void respond() {
      readyToProceedRequests.remove().countDown();
    }
  }

  private static Server createServer(Servlet servlet) {
    Server server = HHServerConnectorTestUtils.createServer(null, servlet);
    server.setStopTimeout(STOP_TIMEOUT_MS);
    return server;
  }

  private static Future<Void> stop(Server server) {
    return executorService.submit(() -> {
      server.stop();
      return null;
    });
  }
}
