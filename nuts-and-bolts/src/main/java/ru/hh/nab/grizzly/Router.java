package ru.hh.nab.grizzly;

import com.google.common.collect.Maps;

import java.util.Map;

public class Router {

  private final Map<String, HandlerDecorator> mapping = Maps.newHashMap();

  public void addRouting(String path, HandlerDecorator handler) {
    if (path.charAt(0) != '/')
      throw new IllegalArgumentException("Path must start with '/'");
    mapping.put(path, handler);
  }

  public HandlerDecorator route(String path) {
    return mapping.get(path);
  }
}
