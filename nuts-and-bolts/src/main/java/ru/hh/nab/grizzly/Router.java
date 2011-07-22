package ru.hh.nab.grizzly;

import java.util.HashMap;
import java.util.Map;

public class Router {

  private final Map<String, RequestHandler> mapping = new HashMap<String, RequestHandler>();

  public void addRouting(String path, RequestHandler handler) {
    if (path.charAt(0) != '/')
      throw new IllegalArgumentException("Path must start with '/'");
    mapping.put(path, handler);
  }

  public RequestHandler route(String path) {
    return mapping.get(path);
  }
}
