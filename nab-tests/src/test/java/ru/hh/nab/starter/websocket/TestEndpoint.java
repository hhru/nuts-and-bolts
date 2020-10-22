package ru.hh.nab.starter.websocket;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/wsNabTest")
public class TestEndpoint {

  public static final String WS_URL = "/wsNabTest";
  private static final String WS_RESPONSE_MESSAGE = "ws response";

  public static volatile String receivedMessage = null;
  public static volatile Boolean connectionOpen = null;

  @OnOpen
  public void onWebSocketOpen(Session session, EndpointConfig endpointConfig) {
    connectionOpen = true;
  }

  @OnMessage
  public String onWebSocketText(String message, Session session) {
    receivedMessage = message;
    return WS_RESPONSE_MESSAGE;
  }

  @OnClose
  public void onWebSocketClose(Session session, CloseReason closeReason) {
    connectionOpen = false;
  }

}
