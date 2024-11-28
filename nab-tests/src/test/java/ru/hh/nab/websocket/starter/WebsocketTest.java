package ru.hh.nab.websocket.starter;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.hh.nab.testbase.web.WebTestBase;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(
    classes = {NabWebTestConfig.class, TestEndpoint.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class WebsocketTest extends WebTestBase {

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
    WebSocketSession session = client.execute(testMessageHandler, resourceHelper.baseUrl("ws") + TestEndpoint.WS_URL).get();
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
}
