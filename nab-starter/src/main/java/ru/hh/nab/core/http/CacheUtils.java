package ru.hh.nab.core.http;

import javax.servlet.http.HttpServletResponse;

public final class CacheUtils {
  public static final String CACHE_CONTROL = "Cache-Control";
  public static final String EXPIRES = "Expires";

  private CacheUtils() {}

  public static void applyCache(HttpServletResponse response, int seconds) {
    if (seconds > 0) {
      response.setDateHeader(EXPIRES, System.currentTimeMillis() + seconds * 1000L);
      response.setHeader(CACHE_CONTROL, "max-age=" + seconds);
    } else {
      noCache(response);
    }
  }

  public static void noCache(HttpServletResponse response) {
    response.setDateHeader(EXPIRES, 1L);
    response.setHeader(CACHE_CONTROL, "must-revalidate,no-cache,no-store");
  }
}
