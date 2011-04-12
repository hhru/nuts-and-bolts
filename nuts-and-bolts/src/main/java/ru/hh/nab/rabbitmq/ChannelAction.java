package ru.hh.nab.rabbitmq;

import com.rabbitmq.client.Channel;

import java.io.IOException;

public interface ChannelAction {
  void perform(Channel channel) throws IOException;
}
