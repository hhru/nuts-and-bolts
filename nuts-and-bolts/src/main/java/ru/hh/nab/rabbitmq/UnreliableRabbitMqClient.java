package ru.hh.nab.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnreliableRabbitMqClient {
  private final Logger log = LoggerFactory.getLogger(UnreliableRabbitMqClient.class);

  private final ConnectionFactory connectionFactory;

  private Connection connection;

  @Inject
  public UnreliableRabbitMqClient(ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  public void maybePerform(ChannelAction action) {
    Connection conn = null;
    try {
      conn = maybeGetConnection();
    } catch (Exception e) {
      log.warn("Can't get connection");
    }
    if (conn == null) {
      return;
    }

    Channel ch = null;
    try {
      ch = conn.createChannel();
    } catch (Exception e) {
      log.warn("Can't get channel");
    }
    if (ch == null) {
      return;
    }

    try {
      action.perform(ch);
    } catch (IOException e) {
      log.warn("IO problem", e);
    } finally {
      try {
        if (ch.isOpen()) {
          ch.close();
        }
      } catch (Exception e) {
        log.warn("Error while closing channel", e);
      }
    }
  }

  private synchronized Connection maybeGetConnection() throws IOException {
    if (connection == null || !connection.isOpen()) {
      connection = connectionFactory.newConnection();
    }
    return connection;
  }
}
