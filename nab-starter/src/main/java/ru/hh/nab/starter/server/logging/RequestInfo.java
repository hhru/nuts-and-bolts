package ru.hh.nab.starter.server.logging;

public final class RequestInfo {
  public static final String LOG_FORMAT =            "%s %s %s %3d ms %s %s";
  public static final String LOG_WITH_CACHE_FORMAT = "%s %s %s %s %3d ms %s %s";

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String EMPTY_REQUEST_ID = "noRequestId";

  public static final String CACHE_ATTRIBUTE = "HttpCache";
  public static final String HIT = "HIT";
  public static final String MISS = "MISS";
  public static final String NO_CACHE = "-";

  private RequestInfo() {}
}
