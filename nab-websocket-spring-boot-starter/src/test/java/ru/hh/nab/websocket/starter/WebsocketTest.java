package ru.hh.nab.websocket.starter;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(classes = WebsocketTest.WebsocketTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebsocketTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testWebsocketConnection() throws ExecutionException, InterruptedException, IOException {
    assertFalse(TestEndpoint.connectionOpen);

    List<String> receivedMessages = new CopyOnWriteArrayList<>();
    var testMessageHandler = new TextWebSocketHandler() {
      @Override
      protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        receivedMessages.add(message.getPayload());
      }
    };

    StandardWebSocketClient client = new StandardWebSocketClient();
    String uriTemplate = UriBuilder.fromUri(testRestTemplate.getRootUri()).scheme("ws").path(TestEndpoint.WS_URL).toTemplate();
    WebSocketSession session = client.execute(testMessageHandler, uriTemplate).get();
    waitUntil(() -> assertTrue(TestEndpoint.connectionOpen));
    assertEquals(0, receivedMessages.size());

    String messageToSend = "привет";
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
  @ImportAutoConfiguration({
      NabWebsocketAutoConfiguration.class,
      WebSocketServletAutoConfiguration.class,
      ServletWebServerFactoryAutoConfiguration.class,
  })
  @Import(TestEndpoint.class)
  public static class WebsocketTestConfiguration {
  }
}
