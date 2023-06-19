package ru.hh.nab.starter.http;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.DateGenerator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class CacheUtilsTest {
  @Test
  public void testCacheControl() {
    HttpServletResponse response = new MockHttpServletResponse();

    CacheUtils.applyCache(response, 100);

    assertEquals("max-age=100", response.getHeader(CacheUtils.CACHE_CONTROL));
    assertNotNull(response.getHeader(CacheUtils.EXPIRES));
  }

  @Test
  public void testCacheControlZero() {
    HttpServletResponse response = new MockHttpServletResponse();

    CacheUtils.applyCache(response, 0);

    assertEquals("must-revalidate,no-cache,no-store", response.getHeader(CacheUtils.CACHE_CONTROL));
    assertEquals(DateGenerator.formatDate(1), response.getHeader(CacheUtils.EXPIRES));
  }

  @Test
  public void testNoCache() {
    HttpServletResponse response = new MockHttpServletResponse();

    CacheUtils.noCache(response);

    assertEquals("must-revalidate,no-cache,no-store", response.getHeader(CacheUtils.CACHE_CONTROL));
    assertEquals(DateGenerator.formatDate(1), response.getHeader(CacheUtils.EXPIRES));
  }
}
