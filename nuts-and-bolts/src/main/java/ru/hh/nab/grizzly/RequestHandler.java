package ru.hh.nab.grizzly;

import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;

public interface RequestHandler {
  void handle(GrizzlyRequest request, GrizzlyResponse response) throws Exception;
}
