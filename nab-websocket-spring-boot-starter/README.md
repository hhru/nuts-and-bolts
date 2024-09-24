### Описание 

Работа с вебсокетами основана на [Java API for WebSocket (JSR 356)](https://docs.oracle.com/javaee/7/tutorial/websocket.htm) стандарте

Чтобы добавить в сервис работу с вебсокетами, нужно:

1) В сервисе добавить зависимость:

```
<dependency>
    <groupId>ru.hh.nab</groupId>
    <artifactId>nab-websocket-spring-boot-starter</artifactId>
    <version>${nab.version}</version>
</dependency>
```

2) Добавить в spring контекст ресурс описывающий нужный endpoint, например

```java

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
```