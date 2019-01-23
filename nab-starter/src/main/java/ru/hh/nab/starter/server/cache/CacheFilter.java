package ru.hh.nab.starter.server.cache;

import com.timgroup.statsd.StatsDClient;
import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;
import org.caffinitas.ohc.OHCacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.CacheControl;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static ru.hh.nab.starter.server.cache.CachedResponse.PLACEHOLDER;
import static ru.hh.nab.starter.server.logging.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.starter.server.logging.RequestInfo.HIT;
import static ru.hh.nab.starter.server.logging.RequestInfo.MISS;

public class CacheFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheFilter.class);
  private static final int NO_CACHE = -1;
  private static final String METRIC = ".http.cache.";
  private static final int STATS_UPDATE_RATE = 15;

  private final String metric;
  private final OHCache<byte[], byte[]> ohCache;
  private final AtomicInteger cachedHits = new AtomicInteger(0);
  private final AtomicInteger cachedMisses = new AtomicInteger(0);
  private final AtomicInteger cachedPlaceholder = new AtomicInteger(0);
  private final AtomicInteger cachedBypass = new AtomicInteger(0);

  public CacheFilter(String serviceName, int size, StatsDClient statsDClient, ScheduledExecutorService scheduledExecutor) {
    metric = serviceName + METRIC;

    Serializer serializer = new Serializer();
    ohCache = OHCacheBuilder.<byte[], byte[]>newBuilder()
        .capacity(size * 1024L * 1024L)
        .timeouts(true)
        .keySerializer(serializer)
        .valueSerializer(serializer)
        .build();

    scheduledExecutor.scheduleAtFixedRate(() -> {
      OHCacheStats stats = ohCache.stats();
      ohCache.resetStatistics();

      statsDClient.count(metric + "internal.hits", stats.getHitCount());
      statsDClient.count(metric + "internal.misses", stats.getMissCount());
      statsDClient.count(metric + "internal.evictions", stats.getEvictionCount());
      // TODO: Fix expires metric reset in ohc
      //statsDClient.count(metric + "internal.expires", stats.getExpireCount());

      statsDClient.count(metric + "put.add", stats.getPutAddCount());
      statsDClient.count(metric + "put.replace", stats.getPutReplaceCount());
      statsDClient.count(metric + "put.fail", stats.getPutFailCount());

      statsDClient.gauge(metric + "capacity", stats.getCapacity());
      statsDClient.gauge(metric + "free", stats.getFree());

      statsDClient.count(metric + "hits", cachedHits.getAndSet(0));
      statsDClient.count(metric + "misses", cachedMisses.getAndSet(0));
      statsDClient.count(metric + "placeholder", cachedPlaceholder.getAndSet(0));
      statsDClient.count(metric + "bypass", cachedBypass.getAndSet(0));
    }, STATS_UPDATE_RATE, STATS_UPDATE_RATE, TimeUnit.SECONDS);
  }

  private static byte[] getCacheKey(HttpServletRequest request) {
    return (request.getMethod() + request.getRequestURI() + '?' + request.getQueryString() + request.getHeader("Accept")).getBytes();
  }

  private static int getMaxAge(HttpServletResponse response) {
    if (response.getStatus() != 200) {
      return NO_CACHE;
    }

    String cacheHeaderValue = response.getHeader(CACHE_CONTROL);
    if (cacheHeaderValue == null) {
      return NO_CACHE;
    }

    try {
      return CacheControl.valueOf(cacheHeaderValue).getMaxAge();
    } catch (IllegalArgumentException e) {
      LOGGER.error("Invalid Cache-Control header value {}", cacheHeaderValue, e);
      return NO_CACHE;
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException { }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

    byte[] key = getCacheKey(httpServletRequest);
    CachedResponse cachedResponse = CachedResponse.from(ohCache.get(key));

    if (cachedResponse == null) {
      filterChain.doFilter(servletRequest, servletResponse);

      int maxAge = getMaxAge(httpServletResponse);
      if (maxAge != NO_CACHE) {
        servletRequest.setAttribute(CACHE_ATTRIBUTE, MISS);
        cachedMisses.incrementAndGet();
        ohCache.putIfAbsent(key, PLACEHOLDER, System.currentTimeMillis() + maxAge * 1000);
      } else {
        cachedBypass.incrementAndGet();
      }
    } else if (cachedResponse.isPlaceholder()) {
      CachingResponseWrapper responseWrapper = new CachingResponseWrapper(httpServletResponse);

      filterChain.doFilter(servletRequest, responseWrapper);
      responseWrapper.flushBuffer();

      int maxAge = getMaxAge(httpServletResponse);
      if (maxAge != NO_CACHE && !responseWrapper.hasError()) {
        servletRequest.setAttribute(CACHE_ATTRIBUTE, MISS);
        cachedPlaceholder.incrementAndGet();
        CachedResponse response = CachedResponse.from(responseWrapper);

        ohCache.put(key, response.getSerialized(), System.currentTimeMillis() + maxAge * 1000);
      } else {
        cachedBypass.incrementAndGet();
      }
    } else {
      servletRequest.setAttribute(CACHE_ATTRIBUTE, HIT);
      cachedHits.incrementAndGet();
      httpServletResponse.setStatus(cachedResponse.status);
      cachedResponse.headers.forEach(header -> httpServletResponse.addHeader(header.header, header.value));

      httpServletResponse.setContentLength(cachedResponse.body.length);
      ServletOutputStream outputStream = httpServletResponse.getOutputStream();
      outputStream.write(cachedResponse.body);
      outputStream.flush();
      httpServletResponse.flushBuffer();
    }
  }

  @Override
  public void destroy() {
    try {
      ohCache.close();
    } catch (IOException e) {
      LOGGER.warn("Unable to close http cache", e);
    }
  }
}
