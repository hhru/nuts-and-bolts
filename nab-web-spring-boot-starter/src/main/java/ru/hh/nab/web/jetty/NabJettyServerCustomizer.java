package ru.hh.nab.web.jetty;

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
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.server.jetty.HHServerConnector;
import ru.hh.nab.starter.server.logging.StructuredRequestLogger;
import ru.hh.nab.web.ExtendedServerProperties;
import ru.hh.nab.web.InfrastructureProperties;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class NabJettyServerCustomizer implements JettyServerCustomizer {

  private final ExtendedServerProperties extendedServerProperties;
  private final InfrastructureProperties infrastructureProperties;
  private final StatsDSender statsDSender;

  public NabJettyServerCustomizer(
      ExtendedServerProperties extendedServerProperties,
      InfrastructureProperties infrastructureProperties,
      StatsDSender statsDSender
  ) {
    this.extendedServerProperties = extendedServerProperties;
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
        newServerConnector.setAcceptQueueSize(extendedServerProperties.getJetty().getAcceptQueueSize());

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
        httpConnectionFactory.getHttpConfiguration().setOutputBufferSize(
            (int) extendedServerProperties.getJetty().getOutputBufferSize().toBytes()
        );
      }
    }
  }

  private void configureRequestLogger(Server server) {
    server.setRequestLog(new StructuredRequestLogger());
  }
}
