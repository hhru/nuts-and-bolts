package ru.hh.nab.starter.server.cache;

import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;
import org.caffinitas.ohc.OHCacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.metrics.StatsDSender;

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
import java.util.concurrent.atomic.AtomicInteger;

import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static ru.hh.nab.starter.server.cache.CachedResponse.PLACEHOLDER;
import static ru.hh.nab.starter.server.logging.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.starter.server.logging.RequestInfo.HIT;
import static ru.hh.nab.starter.server.logging.RequestInfo.MISS;

public class CacheFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheFilter.class);
  private static final int NO_CACHE = -1;
  private static final int STATS_UPDATE_RATE = 15;

  private final OHCache<byte[], byte[]> ohCache;
  private final AtomicInteger cachedHits = new AtomicInteger(0);
  private final AtomicInteger cachedMisses = new AtomicInteger(0);
  private final AtomicInteger cachedPlaceholder = new AtomicInteger(0);
  private final AtomicInteger cachedBypass = new AtomicInteger(0);

  public CacheFilter(String serviceName, int size, StatsDSender statsDSender) {
    Serializer serializer = new Serializer();
    ohCache = OHCacheBuilder.<byte[], byte[]>newBuilder()
        .capacity(size * 1024L * 1024L)
        .timeouts(true)
        .keySerializer(serializer)
        .valueSerializer(serializer)
        .build();

    String internalHitsMetricName = getFullMetricName(serviceName, "internal.hits");
    String internalMissesMetricName = getFullMetricName(serviceName, "internal.misses");
    String internalEvictionsMetricName = getFullMetricName(serviceName, "internal.evictions");
    String putAddMetricName = getFullMetricName(serviceName, "put.add");
    String putReplaceMetricName = getFullMetricName(serviceName, "put.replace");
    String putFailMetricName = getFullMetricName(serviceName, "put.fail");
    String capacityMetricName = getFullMetricName(serviceName, "capacity");
    String freeMetricName = getFullMetricName(serviceName, "free");
    String hitsMetricName = getFullMetricName(serviceName, "hits");
    String missesMetricName = getFullMetricName(serviceName, "misses");
    String placeholderMetricName = getFullMetricName(serviceName, "placeholder");
    String bypassMetricName = getFullMetricName(serviceName, "bypass");

    statsDSender.sendPeriodically(() -> {
      OHCacheStats stats = ohCache.stats();
      ohCache.resetStatistics();

      statsDSender.sendCount(internalHitsMetricName, stats.getHitCount());
      statsDSender.sendCount(internalMissesMetricName, stats.getMissCount());
      statsDSender.sendCount(internalEvictionsMetricName, stats.getEvictionCount());

      statsDSender.sendCount(putAddMetricName, stats.getPutAddCount());
      statsDSender.sendCount(putReplaceMetricName, stats.getPutReplaceCount());
      statsDSender.sendCount(putFailMetricName, stats.getPutFailCount());

      statsDSender.sendGauge(capacityMetricName, stats.getCapacity());
      statsDSender.sendGauge(freeMetricName, stats.getFree());

      statsDSender.sendCount(hitsMetricName, cachedHits.getAndSet(0));
      statsDSender.sendCount(missesMetricName, cachedMisses.getAndSet(0));
      statsDSender.sendCount(placeholderMetricName, cachedPlaceholder.getAndSet(0));
      statsDSender.sendCount(bypassMetricName, cachedBypass.getAndSet(0));
    }, STATS_UPDATE_RATE);
  }

  private static String getFullMetricName(String serviceName, String shortMetricName) {
    return serviceName + ".http.cache." + shortMetricName;
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
