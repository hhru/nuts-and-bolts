package ru.hh.nab.neo.starter.server;

public final class RequestHeaders {
  public static final String REQUEST_ID = "X-Request-Id";
  public static final String EMPTY_REQUEST_ID = "noRequestId";
  public static final String EMPTY_USER_AGENT = "noUserAgent";
  public static final String REQUEST_SOURCE = "x-source";
  public static final String LOAD_TESTING = "x-load-testing";
  private RequestHeaders() {
  }
}
