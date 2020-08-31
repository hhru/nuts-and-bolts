package ru.hh.nab.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SocketsHolder {

  public static Set<EchoSocket> sockets = ConcurrentHashMap.newKeySet();
  public static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  static {
    scheduledExecutorService.scheduleAtFixedRate(() -> sockets.forEach(socket -> {
      ByteBuffer byteBuffer = ByteBuffer.wrap(socket.session.getId().getBytes());
      try {
        socket.remote.sendPing(byteBuffer);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }), 5, 10, TimeUnit.SECONDS);
  }

  public static void addSocket(EchoSocket echoSocket) {
    sockets.add(echoSocket);
  }

}
