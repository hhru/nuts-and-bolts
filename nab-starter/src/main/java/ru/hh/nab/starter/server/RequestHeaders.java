package ru.hh.nab.starter.server;

public final class RequestHeaders {
  private RequestHeaders() {
  }
  public static final String REQUEST_ID = "X-Request-Id";
  public static final String EMPTY_REQUEST_ID = "noRequestId";
  public static final String REQUEST_SOURCE = "x-source";
  public static final String LOAD_TESTING = "x-load-testing";
}
