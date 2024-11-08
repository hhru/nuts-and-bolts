package ru.hh.nab.web.jetty;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class SimpleAsyncHTTPClient {

  private final ExecutorService executorService;

  SimpleAsyncHTTPClient(ExecutorService executorService) {
    this.executorService = executorService;
  }

  Future<Integer> request(Socket socket) throws IOException {
    PrintWriter socketWriter = new PrintWriter(socket.getOutputStream());
    socketWriter.println("GET / HTTP/1.1");
    socketWriter.println("Host: localhost");
    socketWriter.println();
    socketWriter.flush();

    BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    Callable<Integer> responseStatusReader = () -> {
      String threadOldName = Thread.currentThread().getName();
      Thread.currentThread().setName("response reader from " + socket.getRemoteSocketAddress());
      try {
        String firstLine = socketReader.readLine();
        if (firstLine == null) {
          throw new EOFException();
        }
        int responseStatus = Integer.parseInt(firstLine.split(" ")[1]);

        while (true) {
          String line = socketReader.readLine();
          if (line == null || line.isEmpty()) {
            break;
          }
        }

        return responseStatus;

      } finally {
        Thread.currentThread().setName(threadOldName);
      }
    };
    return executorService.submit(responseStatusReader);
  }

}
