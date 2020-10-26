package ru.hh.nab.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketSessionsHandler {

  private static final Logger logger = LoggerFactory.getLogger(WebsocketSessionsHandler.class);

  public Map<Session, LocalDateTime> sessionsTtl;
  public ScheduledExecutorService scheduledExecutorService;

  @Inject
  public WebsocketSessionsHandler() {
    sessionsTtl = new ConcurrentHashMap<>();

    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(() -> sessionsTtl.forEach((session, ttl) -> {
      ByteBuffer byteBuffer = ByteBuffer.wrap(session.getId().getBytes());
      try {
        session.getAsyncRemote().sendPing(byteBuffer);
      } catch (IOException e) {
        logger.warn("exception during websocket ping", e);
      }
    }), 5, 10, TimeUnit.SECONDS);

    scheduledExecutorService.scheduleAtFixedRate(() -> {
      LocalDateTime now = LocalDateTime.now();
      sessionsTtl.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }, 10, 5, TimeUnit.SECONDS);

    scheduledExecutorService.scheduleAtFixedRate(() -> {
      logger.info("has {} sessions active", sessionsTtl.size());
    }, 10, 15, TimeUnit.SECONDS);

  }

  public void addSocket(Session session) {
    sessionsTtl.put(session, LocalDateTime.now().plusSeconds(30));
  }

  public void closeSocket(Session session) {
    sessionsTtl.remove(session);
  }

  public void handlePong(Session session) {
    sessionsTtl.put(session, LocalDateTime.now().plusSeconds(30));
  }

}
