package ru.hh.nab.starter.consul;

import java.time.Duration;
import ru.hh.consul.cache.CacheDescriptor;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.nab.metrics.Counters;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import static ru.hh.nab.starter.consul.ConsulMetrics.CACHE_POLLS;
import static ru.hh.nab.starter.consul.ConsulMetrics.REQUESTS;

public class ConsulMetricsTracker implements ClientEventCallback {

  private static final String SUCCESSFUL_RESULT = "success";
  private static final String FAILED_RESULT = "failure";

  private final String serviceName;
  private final Counters requestCounters, cachePollCounters;

  public ConsulMetricsTracker(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;

    this.requestCounters = new Counters(500);
    this.cachePollCounters = new Counters(500);

    statsDSender.sendPeriodically(() -> {
      statsDSender.sendCounters(REQUESTS, requestCounters);
      statsDSender.sendCounters(CACHE_POLLS, cachePollCounters);
    });
  }

  @Override
  public void onHttpRequestSuccess(String clientName, String method, String path, String queryString, int responseCode) {
    Tag[] tags = createHttpRequestTags(clientName, method, path, SUCCESSFUL_RESULT, String.valueOf(responseCode));
    requestCounters.add(1, tags);
  }

  @Override
  public void onHttpRequestFailure(String clientName, String method, String path, String queryString, Throwable throwable) {
    Tag[] tags = createHttpRequestTags(clientName, method, path, FAILED_RESULT, throwable.getClass().getSimpleName());
    requestCounters.add(1, tags);
  }

  @Override
  public void onHttpRequestInvalid(String clientName, String method, String path, String queryString, int responseCode, Throwable throwable) {
    Tag[] tags = createHttpRequestTags(clientName, method, path, FAILED_RESULT, String.valueOf(responseCode));
    requestCounters.add(1, tags);
  }

  @Override
  public void onCachePollingError(String clientName, CacheDescriptor cacheDescriptor, Throwable throwable) {
    Tag[] tags = createCachePollingTags(clientName, cacheDescriptor, FAILED_RESULT);
    cachePollCounters.add(1, tags);
  }

  @Override
  public void onCachePollingSuccess(String clientName, CacheDescriptor cacheDescriptor, boolean withNotification, Duration duration) {
    Tag[] tags = createCachePollingTags(clientName, cacheDescriptor, SUCCESSFUL_RESULT);
    cachePollCounters.add(1, tags);
  }

  private Tag[] createHttpRequestTags(String clientName, String method, String path, String result, String type) {
    return new Tag[]{
        new Tag(APP_TAG_NAME, serviceName),
        new Tag("client", clientName),
        new Tag("method", method),
        new Tag("url", path),
        new Tag("result", result),
        new Tag("type", type),
    };
  }

  private Tag[] createCachePollingTags(String clientName, CacheDescriptor cacheDescriptor, String result) {
    return new Tag[]{
        new Tag(APP_TAG_NAME, serviceName),
        new Tag("client", clientName),
        new Tag("endpoint", cacheDescriptor.getEndpoint()),
        new Tag("key", cacheDescriptor.getKey()),
        new Tag("result", result)
    };
  }
}
