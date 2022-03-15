package ru.hh.nab.neo.starter.server;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.util.unit.DataSize;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import ru.hh.nab.metrics.TaggedSender;
import ru.hh.nab.neo.starter.props.NabProperties;

public class NabJettyWebServerFactoryCustomizer
    implements WebServerFactoryCustomizer<JettyServletWebServerFactory>, Ordered {
  private final StatsDSender statsDSender;
  private final String serviceName;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final ServerProperties serverProperties;
  private final NabProperties nabProperties;
  private final ThreadPool jettyThreadPool;

  public NabJettyWebServerFactoryCustomizer(StatsDSender statsDSender,
                                            String serviceName,
                                            ApplicationEventPublisher applicationEventPublisher,
                                            ServerProperties serverProperties,
                                            ThreadPool jettyThreadPool,
                                            NabProperties nabProperties) {
    this.statsDSender = statsDSender;
    this.serviceName = serviceName;
    this.applicationEventPublisher = applicationEventPublisher;
    this.serverProperties = serverProperties;
    this.jettyThreadPool = jettyThreadPool;
    this.nabProperties = nabProperties;
  }

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    factory.addConfigurations(new AbstractConfiguration() {
      @Override
      public void configure(WebAppContext context) {
        System.out.println();
      }
    });
    factory.setThreadPool(jettyThreadPool);
    factory.addServerCustomizers(server -> {
      server.addLifeCycleListener(new JettyLifeCycleListener(applicationEventPublisher));
      // Здесь именно set, чтобы дефолтный коннектор затереть.
      server.setConnectors(new ServerConnector[]{configureConnector(server)});
      server.setStopTimeout(nabProperties.getServer().getStopTimeoutMs());
    });
  }

  @Override
  public int getOrder() {
    //default boot customizer (JettyWebServerFactoryCustomizer) has 0
    return 1;
  }

  private ServerConnector configureConnector(Server server) {
    ServerConnector serverConnector = new HHServerConnector(
        server,
        serverProperties.getJetty().getThreads().getAcceptors(),
        serverProperties.getJetty().getThreads().getSelectors(),
        new TaggedSender(statsDSender, Set.of(new Tag(APP_TAG_NAME, serviceName))), createHttpConnectionFactory());
//    serverConnector.setHost(jettySettings.getString(HOST));
    serverConnector.setPort(serverProperties.getPort());
    serverConnector.setIdleTimeout(
        Optional.ofNullable(serverProperties.getJetty().getConnectionIdleTimeout()).map(Duration::toMillis).orElse(3_000L)
    );
    serverConnector.setAcceptQueueSize(
        Optional.ofNullable(serverProperties.getJetty().getThreads().getMaxQueueCapacity()).orElse(50)
    );
    return serverConnector;
  }

  private HttpConnectionFactory createHttpConnectionFactory() {
    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    NabProperties.Server nabServerProperties = nabProperties.getServer();
    httpConfiguration.setSecurePort(nabServerProperties.getSecurePort());
    httpConfiguration.setOutputBufferSize((int) nabServerProperties.getOutputBufferSize().toBytes());

    httpConfiguration.setResponseHeaderSize((int) nabServerProperties.getResponseHeaderSize().toBytes());

    httpConfiguration.setRequestHeaderSize(Optional.ofNullable(serverProperties.getMaxHttpHeaderSize())
        .map(DataSize::toBytes)
        .map(Long::intValue)
        .orElse(16384)
    );

    httpConfiguration.setSendServerVersion(false);
    // я не понимаю как таймаут можно заменить на темп.
    // org.eclipse.jetty.server.HttpConfiguration.setMinResponseDataRate будет отрывать соединение не через 5 секунд, а уже после первой передачи,
    // если темп в пересчете на секунду окажется меньше, чем указано. какое-то говно
    httpConfiguration.setBlockingTimeout(5000);
    return new HttpConnectionFactory(httpConfiguration);
  }
}
