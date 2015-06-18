package ru.hh.nab.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UnreliableRabbitMqClientTest {

  @Test public void connectAndPublishNormally() throws IOException {
    // given
    Channel channel = mock(Channel.class);

    Connection connection = mock(Connection.class);
    given(connection.createChannel()).willReturn(channel);

    ConnectionFactory factory = mock(ConnectionFactory.class);
    given(factory.newConnection()).willReturn(connection);

    UnreliableRabbitMqClient mqClient = new UnreliableRabbitMqClient(factory);

    ChannelAction action = mock(ChannelAction.class);
    willAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Channel ch = (Channel) invocation.getArguments()[0];
        ch.basicPublish("exchange", "routingKey", false, false, new AMQP.BasicProperties(), new byte[]{});
        return null;
      }
    }).given(action).perform(channel);

    // when
    mqClient.maybePerform(action);

    // then
    verify(action).perform(channel);
    verify(channel).basicPublish(anyString(), anyString(), anyBoolean(), anyBoolean(), Matchers.<AMQP.BasicProperties>any(), Matchers.<byte[]>any());
  }

  @Test public void failedToConnect() throws IOException {
    // given
    ConnectionFactory factory = mock(ConnectionFactory.class);
    given(factory.newConnection()).willThrow(new RuntimeException("Some broker problem"));

    UnreliableRabbitMqClient mqClient = new UnreliableRabbitMqClient(factory);
    ChannelAction action = mock(ChannelAction.class);

    // when
    mqClient.maybePerform(action);

    // then
    Mockito.verify(action, never()).perform(Matchers.<Channel>any());
  }

  @Test public void failedToOpenChannel() throws IOException {
    // given
    Connection connection = mock(Connection.class);
    given(connection.createChannel()).willThrow(new RuntimeException("Some broker problem"));
    ConnectionFactory factory = mock(ConnectionFactory.class);
    given(factory.newConnection()).willReturn(connection);

    UnreliableRabbitMqClient mqClient = new UnreliableRabbitMqClient(factory);
    ChannelAction action = mock(ChannelAction.class);

    // when
    mqClient.maybePerform(action);

    // then
    Mockito.verify(action, never()).perform(Matchers.<Channel>any());
  }

  @Test public void failedToPublish() throws IOException {
    // given
    Channel channel = mock(Channel.class);
    doThrow(new IOException("Some connection problem"))
        .when(channel)
        .basicPublish(anyString(), anyString(), anyBoolean(), anyBoolean(), Matchers.<AMQP.BasicProperties>any(), Matchers.<byte[]>any());
    given(channel.isOpen()).willReturn(true);

    Connection connection = mock(Connection.class);
    given(connection.createChannel()).willReturn(channel);
    ConnectionFactory factory = mock(ConnectionFactory.class);
    given(factory.newConnection()).willReturn(connection);

    UnreliableRabbitMqClient mqClient = new UnreliableRabbitMqClient(factory);
    ChannelAction action = mock(ChannelAction.class);

    willAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Channel ch = (Channel) invocation.getArguments()[0];
        try {
          ch.basicPublish("exchange", "routingKey", false, false, new AMQP.BasicProperties(), new byte[]{});
          fail("Expected exception");
        } catch (IOException expected) {}
        return null;
      }
    }).given(action).perform(channel);

    // when
    mqClient.maybePerform(action);

    // then
    verify(channel).close();
  }

  @Test public void failedToPerformAction() throws IOException {
    // given
    Channel channel = mock(Channel.class);
    given(channel.isOpen()).willReturn(true);

    Connection connection = mock(Connection.class);
    given(connection.createChannel()).willReturn(channel);
    ConnectionFactory factory = mock(ConnectionFactory.class);
    given(factory.newConnection()).willReturn(connection);

    UnreliableRabbitMqClient mqClient = new UnreliableRabbitMqClient(factory);
    ChannelAction action = mock(ChannelAction.class);

    willThrow(new RuntimeException("Problem in action")).given(action).perform(channel);

    // when
    try {
      mqClient.maybePerform(action);
      fail("Expected exception");
    } catch (RuntimeException expected) {}

    // then
    verify(channel).close();
  }
}
