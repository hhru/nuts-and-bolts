package ru.hh.nab.web.starter.jetty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.web.jetty.HHServerConnector;
import ru.hh.nab.web.jetty.StructuredRequestLogger;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class NabJettyServerCustomizer implements JettyServerCustomizer {

  public final static String JETTY_ACCEPT_QUEUE_SIZE_PROPERTY = "server.jetty.accept-queue-size";
  public final static String JETTY_OUTPUT_BUFFER_SIZE_PROPERTY = "server.jetty.output-buffer-size";

  public final static int DEFAULT_JETTY_ACCEPT_QUEUE_SIZE = 50;
  public final static int DEFAULT_JETTY_OUTPUT_BUFFER_SIZE = (int) DataSize.ofKilobytes(64).toBytes();

  private final Environment environment;
  private final InfrastructureProperties infrastructureProperties;
  private final StatsDSender statsDSender;

  public NabJettyServerCustomizer(
      Environment environment,
      InfrastructureProperties infrastructureProperties,
      StatsDSender statsDSender
  ) {
    this.environment = environment;
    this.infrastructureProperties = infrastructureProperties;
    this.statsDSender = statsDSender;
  }

  @Override
  public void customize(Server server) {
    configureConnectors(server);
    configureRequestLogger(server);
  }

  private void configureConnectors(Server server) {
    List<Connector> connectors = new ArrayList<>();
    for (Connector connector : server.getConnectors()) {
      configureConnectionFactories(connector.getConnectionFactories());
      if (connector instanceof ServerConnector serverConnector) {
        ServerConnector newServerConnector = new HHServerConnector(
            server,
            serverConnector.getExecutor(),
            serverConnector.getScheduler(),
            serverConnector.getByteBufferPool(),
            serverConnector.getAcceptors(),
            serverConnector.getSelectorManager().getSelectorCount(),
            statsDSender,
            infrastructureProperties.getServiceName(),
            serverConnector.getConnectionFactories().toArray(ConnectionFactory[]::new)
        );

        newServerConnector.setHost(serverConnector.getHost());
        newServerConnector.setPort(serverConnector.getPort());
        newServerConnector.setAcceptQueueSize(environment.getProperty(
            JETTY_ACCEPT_QUEUE_SIZE_PROPERTY,
            Integer.class,
            DEFAULT_JETTY_ACCEPT_QUEUE_SIZE
        ));

        connectors.add(newServerConnector);
      } else {
        connectors.add(connector);
      }
    }
    server.setConnectors(connectors.toArray(Connector[]::new));
  }

  private void configureConnectionFactories(Collection<ConnectionFactory> connectionFactories) {
    for (ConnectionFactory connectionFactory : connectionFactories) {
      if (connectionFactory instanceof HttpConfiguration.ConnectionFactory httpConnectionFactory) {
        int outputBufferSize = environment.getProperty(JETTY_OUTPUT_BUFFER_SIZE_PROPERTY, Integer.class, DEFAULT_JETTY_OUTPUT_BUFFER_SIZE);
        httpConnectionFactory.getHttpConfiguration().setOutputBufferSize(outputBufferSize);
      }
    }
  }

  private void configureRequestLogger(Server server) {
    server.setRequestLog(new StructuredRequestLogger());
  }
}
