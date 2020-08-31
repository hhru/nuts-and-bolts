package ru.hh.nab.example;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/echo")
public class EchoSocket {
  private static final Logger LOG = LoggerFactory.getLogger(EchoSocket.class);
  public Session session;
  public RemoteEndpoint.Async remote;

  @OnClose
  public void onWebSocketClose(CloseReason close) {
    this.session = null;
    this.remote = null;
    LOG.info("WebSocket Close: {} - {}", close.getCloseCode(), close.getReasonPhrase());
    SocketsHolder.sockets.remove(this);
  }

  @OnOpen
  public void onWebSocketOpen(Session session) {
    this.session = session;
    this.remote = this.session.getAsyncRemote();
    LOG.info("WebSocket Connect: {}", session);
    this.remote.sendText("You are now connected to " + this.getClass().getName());
    SocketsHolder.sockets.add(this);
  }

  @OnError
  public void onWebSocketError(Throwable cause) {
    LOG.warn("WebSocket Error", cause);
  }

  @OnMessage
  public String onWebSocketText(String message) {
    LOG.info("Echoing back text message [{}]", message);
    return message;
  }

  @OnMessage
  public void onPong(PongMessage pongMessage) {
    LOG.info("Got pong message [{}]", new String(pongMessage.getApplicationData().array()));
  }
}
