package ru.hh.nab.starter.http;

public class RequestContext {
  private RequestContext() {}

  private static final ThreadLocal<String> REQUEST_SOURCE = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> LOAD_TESTING = new ThreadLocal<>();
  private static final ThreadLocal<Long> OUTER_TIMEOUT = new ThreadLocal<>();

  public static String getRequestSource() {
    return REQUEST_SOURCE.get();
  }

  public static void setRequestSource(String source) {
    REQUEST_SOURCE.set(source);
  }

  public static void clearRequestSource() {
    REQUEST_SOURCE.remove();
  }

  public static boolean isLoadTesting() {
    return Boolean.TRUE.equals(LOAD_TESTING.get());
  }

  public static void setLoadTesting(boolean isLoadTesting) {
    LOAD_TESTING.set(isLoadTesting);
  }

  public static void clearLoadTesting() {
    LOAD_TESTING.remove();
  }

  public static Long getOuterTimeoutMs() {
    return OUTER_TIMEOUT.get();
  }

  public static void setOuterTimeoutMs(Long outerTimeoutMs) {
    OUTER_TIMEOUT.set(outerTimeoutMs);
  }

  public static void clearOuterTimeout() {
    OUTER_TIMEOUT.remove();
  }
}
