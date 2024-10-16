package ru.hh.nab.starter.server.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;
import org.caffinitas.ohc.OHCacheStats;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;
import ru.hh.nab.starter.jersey.NabPriorities;
import static ru.hh.nab.starter.server.cache.CachedResponse.PLACEHOLDER;
import static ru.hh.nab.starter.server.logging.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.starter.server.logging.RequestInfo.HIT;
import static ru.hh.nab.starter.server.logging.RequestInfo.MISS;

@Priority(NabPriorities.CACHE)
public class CacheFilter implements ContainerRequestFilter, ContainerResponseFilter, WriterInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheFilter.class);
  private static final int NO_CACHE = -1;
  private static final int STATS_UPDATE_RATE = 15;

  private static final String REQUEST_KEY_PROPERTY = CacheFilter.class.getName() + ".requestKey";
  private static final String CACHED_RESPONSE_STATE_PROPERTY = CacheFilter.class.getName() + ".cachedResponseState";
  private static final String RESPONSE_STATUS_PROPERTY = CacheFilter.class.getName() + ".responseStatus";

  private final OHCache<byte[], byte[]> cache;
  private final AtomicInteger cachedHits = new AtomicInteger(0);
  private final AtomicInteger cachedMisses = new AtomicInteger(0);
  private final AtomicInteger cachedPlaceholder = new AtomicInteger(0);
  private final AtomicInteger cachedBypass = new AtomicInteger(0);

  @Inject
  private HttpServletResponse servletResponse;

  @Context
  private MessageBodyWorkers messageBodyWorkers;

  @Context
  private PropertiesDelegate propertiesDelegate;

  @Context
  private Configuration configuration;

  public CacheFilter(String serviceName, int size, StatsDSender statsDSender) {
    Serializer serializer = new Serializer();
    cache = OHCacheBuilder
        .<byte[], byte[]>newBuilder()
        .capacity(size * 1024L * 1024L)
        .timeouts(true)
        .keySerializer(serializer)
        .valueSerializer(serializer)
        .build();

    String internalHitsMetricName = "http.cache.internal.hits";
    String internalMissesMetricName = "http.cache.internal.misses";
    String internalEvictionsMetricName = "http.cache.internal.evictions";
    String putAddMetricName = "http.cache.put.add";
    String putReplaceMetricName = "http.cache.put.replace";
    String putFailMetricName = "http.cache.put.fail";
    String capacityMetricName = "http.cache.capacity";
    String freeMetricName = "http.cache.free";
    String hitsMetricName = "http.cache.hits";
    String missesMetricName = "http.cache.misses";
    String placeholderMetricName = "http.cache.placeholder";
    String bypassMetricName = "http.cache.bypass";
    var sender = new TaggedSender(statsDSender, Set.of(new Tag(Tag.APP_TAG_NAME, serviceName)));

    statsDSender.sendPeriodically(() -> {
      OHCacheStats stats = cache.stats();
      cache.resetStatistics();

      sender.sendCount(internalHitsMetricName, stats.getHitCount());
      sender.sendCount(internalMissesMetricName, stats.getMissCount());
      sender.sendCount(internalEvictionsMetricName, stats.getEvictionCount());

      sender.sendCount(putAddMetricName, stats.getPutAddCount());
      sender.sendCount(putReplaceMetricName, stats.getPutReplaceCount());
      sender.sendCount(putFailMetricName, stats.getPutFailCount());

      sender.sendGauge(capacityMetricName, stats.getCapacity());
      sender.sendGauge(freeMetricName, stats.getFree());

      sender.sendCount(hitsMetricName, cachedHits.getAndSet(0));
      sender.sendCount(missesMetricName, cachedMisses.getAndSet(0));
      sender.sendCount(placeholderMetricName, cachedPlaceholder.getAndSet(0));
      sender.sendCount(bypassMetricName, cachedBypass.getAndSet(0));
    }, STATS_UPDATE_RATE);
  }

  private byte[] getCacheKey(ContainerRequestContext requestContext) {
    String method = requestContext.getMethod();
    String path = requestContext.getUriInfo().getRequestUri().getRawPath();
    String rawQuery = Optional
        .ofNullable(requestContext.getUriInfo().getRequestUri().getRawQuery())
        .map(query -> '?' + query)
        .orElse("");
    String acceptHeader = Optional
        .ofNullable(requestContext.getHeaderString("Accept"))
        .map(header -> '#' + header)
        .orElse("");
    return (method + path + rawQuery + acceptHeader).getBytes();
  }

  private int getMaxAge(MultivaluedMap<String, String> headers) {
    Optional<String> cacheHeaderValue = ofNullable(headers.get(CACHE_CONTROL))
        .filter(cacheControlHeaderValues -> !cacheControlHeaderValues.isEmpty())
        .map(cacheControlHeaderValues -> cacheControlHeaderValues.get(0));
    if (cacheHeaderValue.isEmpty()) {
      return NO_CACHE;
    }

    try {
      return CacheControl.valueOf(cacheHeaderValue.get()).getMaxAge();
    } catch (IllegalArgumentException e) {
      LOGGER.error("Invalid Cache-Control header value {}", cacheHeaderValue, e);
      return NO_CACHE;
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    byte[] key = getCacheKey(requestContext);
    CachedResponse cachedResponse = CachedResponse.from(cache.get(key));
    requestContext.setProperty(REQUEST_KEY_PROPERTY, key);
    if (cachedResponse == null) {
      requestContext.setProperty(CACHED_RESPONSE_STATE_PROPERTY, CachedResponseState.NOT_CACHED);
    } else if (cachedResponse.isPlaceholder()) {
      requestContext.setProperty(CACHED_RESPONSE_STATE_PROPERTY, CachedResponseState.PLACEHOLDER_CACHED);
    } else {
      requestContext.setProperty(CACHED_RESPONSE_STATE_PROPERTY, CachedResponseState.CACHED);
      requestContext.setProperty(CACHE_ATTRIBUTE, HIT);
      cachedHits.incrementAndGet();
      Response.ResponseBuilder responseBuilder = Response.status(cachedResponse.status);
      cachedResponse.headers.forEach(header -> responseBuilder.header(header.header, header.value));
      responseBuilder.entity(new ByteArrayInputStream(cachedResponse.body));
      requestContext.abortWith(responseBuilder.build());
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    requestContext.setProperty(RESPONSE_STATUS_PROPERTY, responseContext.getStatus());
  }

  @Override
  public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
    byte[] key = (byte[]) context.getProperty(REQUEST_KEY_PROPERTY);
    CachedResponseState cachedResponseState = (CachedResponseState) context.getProperty(CACHED_RESPONSE_STATE_PROPERTY);
    int responseStatus = (int) context.getProperty(RESPONSE_STATUS_PROPERTY);

    if (cachedResponseState == CachedResponseState.NOT_CACHED || cachedResponseState == CachedResponseState.PLACEHOLDER_CACHED) {
      MultivaluedMap<String, String> headers = getHeaders(servletResponse, context);
      int maxAge = getMaxAge(headers);
      if (responseStatus == Response.Status.OK.getStatusCode() && maxAge > 0) {
        context.setProperty(CACHE_ATTRIBUTE, MISS);
        if (cachedResponseState == CachedResponseState.NOT_CACHED) {
          // just cache placeholder and call next write interceptors
          cachedMisses.incrementAndGet();
          cache.putIfAbsent(key, PLACEHOLDER, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAge));
          context.proceed();
        } else {
          // call next write interceptors and cache result after all interceptors made their work
          CachingOutputStream cachingOutputStream = new CachingOutputStream(context.getOutputStream());
          context.setOutputStream(cachingOutputStream);
          context.proceed();
          cachingOutputStream.flush();

          cachedPlaceholder.incrementAndGet();
          CachedResponse response = CachedResponse.from(responseStatus, headers, cachingOutputStream.getContentAsByteArray());
          cache.put(key, response.getSerialized(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAge));
        }
      } else {
        cachedBypass.incrementAndGet();
        context.proceed();
      }
    } else {
      // write cached result to entity output stream without calling any write interceptors
      messageBodyWorkers.writeTo(
          context.getEntity(),
          context.getType(),
          context.getGenericType(),
          context.getAnnotations(),
          context.getMediaType(),
          context.getHeaders(),
          propertiesDelegate,
          context.getOutputStream(),
          List.of()
      );
    }
  }

  private MultivaluedMap<String, String> getHeaders(HttpServletResponse servletResponse, WriterInterceptorContext context) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    servletResponse.getHeaderNames().forEach(
        headerName -> headers.put(headerName, servletResponse.getHeaders(headerName).stream().toList())
    );
    headers.putAll(HeaderUtils.asStringHeaders(context.getHeaders(), configuration));
    return headers;
  }

  private enum CachedResponseState {
    NOT_CACHED, PLACEHOLDER_CACHED, CACHED
  }
}
