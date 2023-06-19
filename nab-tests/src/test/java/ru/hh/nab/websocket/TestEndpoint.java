package ru.hh.nab.websocket;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(TestEndpoint.WS_URL)
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
