package ru.hh.nab.starter.consul;

import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.nab.metrics.Counters;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;

public class ConsulMetricsTracker implements ClientEventCallback {

  private static final String CONSUL_REQUESTS_METRIC = "consul-client.request";
  private static final String SUCCESSFUL_RESULT = "success";
  private static final String FAILED_RESULT = "failure";

  private final String serviceName;
  private final Counters requestCounters;

  public ConsulMetricsTracker(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.requestCounters = new Counters(500);

    statsDSender.sendPeriodically(() -> statsDSender.sendCounters(CONSUL_REQUESTS_METRIC, requestCounters));
  }

  @Override
  public void onHttpRequestSuccess(String clientName, String method, String path, String queryString, int responseCode) {
    Tag[] tags = createHttpRequestTags(SUCCESSFUL_RESULT, String.valueOf(responseCode));
    requestCounters.add(1, tags);
  }

  @Override
  public void onHttpRequestFailure(String clientName, String method, String path, String queryString, Throwable throwable) {
    Tag[] tags = createHttpRequestTags(FAILED_RESULT, throwable.getClass().getSimpleName());
    requestCounters.add(1, tags);
  }

  @Override
  public void onHttpRequestInvalid(String clientName, String method, String path, String queryString, int responseCode, Throwable throwable) {
    Tag[] tags = createHttpRequestTags(FAILED_RESULT, String.valueOf(responseCode));
    requestCounters.add(1, tags);
  }

  private Tag[] createHttpRequestTags(String result, String type) {
    return new Tag[]{
        new Tag(APP_TAG_NAME, serviceName),
        new Tag("result", result),
        new Tag("type", type),
    };
  }
}
