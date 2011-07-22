package ru.hh.nab.grizzly;

import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;

@Route(path = "/login")
public class LoginHandler implements RequestHandler {
  @Override
  public void handle(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
  }
}
