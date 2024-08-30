package ru.hh.nab.starter.server.jetty;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import ru.hh.nab.metrics.TaggedSender;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.getPort;
import static ru.hh.nab.starter.server.jetty.HHServerConnectorTestUtils.repeat;

public class HHServerConnectorGracefulTest {

  private static final int STOP_TIMEOUT_MS = 10_000;

  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  private static final SimpleAsyncHTTPClient httpClient = new SimpleAsyncHTTPClient(executorService);
  private static final TaggedSender statsDSender = mock(TaggedSender.class);

  @AfterAll
  public static void afterGracefulServletContextHandlerTestClass() {
    executorService.shutdown();
  }

  @Test
  public void testHHServerConnectorWaitsPendingRequests() throws Exception {
    repeat(50, () -> {
      ControlledServlet controlledServlet = new ControlledServlet(204);
      Server server = createServer(controlledServlet);
      server.addConnector(new HHServerConnector(server, statsDSender));
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
}
