package ru.hh.nab.example;

import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.server.standard.SpringConfigurator;

@ServerEndpoint(value = "/wsEchoEndpoint", configurator = SpringConfigurator.class)
public class WebsocketEchoEndpoint {
  private static final Logger LOG = LoggerFactory.getLogger(WebsocketEchoEndpoint.class);

  private final WebsocketSessionsHandler websocketSessionsHandler;

  @Inject
  public WebsocketEchoEndpoint(WebsocketSessionsHandler websocketSessionsHandler) {
    this.websocketSessionsHandler = websocketSessionsHandler;
  }

  @OnClose
  public void onWebSocketClose(Session session, CloseReason closeReason) {
    LOG.info("WebSocket Close: {} - {}", closeReason.getCloseCode(), closeReason.getReasonPhrase());
    websocketSessionsHandler.closeSocket(session);
  }

  @OnOpen
  public void onWebSocketOpen(Session session, EndpointConfig endpointConfig) {
    websocketSessionsHandler.addSocket(session);
  }

  @OnError
  public void onWebSocketError(Throwable cause) {
    LOG.warn("WebSocket Error", cause);
  }

  @OnMessage
  public String onWebSocketText(String message, Session session) {
    LOG.info("Echoing back text message [{}]", message);
    return message;
  }

  @OnMessage
  public void onPong(PongMessage pongMessage, Session session) {
    websocketSessionsHandler.handlePong(session);
  }
}
