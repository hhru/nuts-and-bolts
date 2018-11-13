package ru.hh.nab.testbase.spring;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.Function;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DefaultTestContext;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.testbase.JettyTestContainer;
import ru.hh.nab.testbase.NabRunner;
import ru.hh.nab.testbase.NabTestBase;

public class NabTestContext extends DefaultTestContext {

  private final NabRunner.NabBootstrapper nabBootstrapper;
  private PortHolder port;
  private JettyTestContainer jettyTestContainer;

  public NabTestContext(TestContext testContext, NabRunner.NabBootstrapper nabBootstrapper) {
    super((DefaultTestContext) testContext);
    this.nabBootstrapper = nabBootstrapper;
  }

  public PortHolder getPortHolder() {
    if (port == null) {
      port = nabBootstrapper.getPort((Class<? extends NabTestBase>) getTestClass());
    }
    return port;
  }

  public JettyTestContainer getJetty(WebApplicationContext applicationContext) {
    if (jettyTestContainer == null) {
      jettyTestContainer = nabBootstrapper.getJetty((NabTestBase) getTestInstance(), applicationContext);
    }
    return jettyTestContainer;
  }

  public static final class PortHolder {
    private final ServerSocket serverSocket;

    public PortHolder() {
      try {
        serverSocket = new ServerSocket(0);
      } catch (IOException e) {
        throw new RuntimeException("Error on reserving port for server", e);
      }
    }

    public int getPort() {
      return serverSocket.getLocalPort();
    }

    public <S> S releaseAndApply(Function<Integer, S> action) {
      try {
        int port = getPort();
        serverSocket.close();
        return action.apply(port);
      } catch (IOException e) {
        throw new RuntimeException("Error on reserving port for server", e);
      }
    }
  }
}
