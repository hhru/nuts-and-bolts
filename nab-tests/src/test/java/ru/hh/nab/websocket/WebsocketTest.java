package ru.hh.nab.websocket;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig({NabTestConfig.class, WebsocketTest.WebsocketCtx.class})
public class WebsocketTest {

  @NabTestServer(overrideApplication = WebsocketCtx.class)
  ResourceHelper resourceHelper;

  @Test
  public void testWebsocketConnection() throws ExecutionException, InterruptedException, IOException {
    assertNull(TestEndpoint.connectionOpen);

    List<String> receivedMessages = new CopyOnWriteArrayList();
    var testMessageHandler = new TextWebSocketHandler() {
      @Override
      protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        receivedMessages.add(message.getPayload());
      }
    };
    String messageToSend = "привет";

    StandardWebSocketClient client = new StandardWebSocketClient();
    WebSocketSession session = client.doHandshake(testMessageHandler, resourceHelper.baseUrl("ws") + TestEndpoint.WS_URL).completable().get();
    waitUntil(() -> assertTrue(TestEndpoint.connectionOpen));

    assertEquals(0, receivedMessages.size());
    session.sendMessage(new TextMessage(messageToSend));
    waitUntil(() -> {
      assertEquals(1, receivedMessages.size());
      assertEquals(messageToSend, TestEndpoint.receivedMessage);
    });

    session.close();
    waitUntil(() -> assertFalse(TestEndpoint.connectionOpen));
  }

  private void waitUntil(Runnable assertion) {
    await().atMost(3, TimeUnit.SECONDS).untilAsserted(assertion::run);
  }

  @Configuration
  @Import(TestEndpoint.class)
  public static class WebsocketCtx implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder()
          .apply(builder -> NabWebsocketConfigurator.configureWebsocket(builder, Set.of("ru.hh")))
          .build();
    }
  }

}
