package ru.hh.nab.jclient.metrics;

import java.util.HashMap;
import java.util.Map;
import static java.util.Objects.requireNonNullElse;
import ru.hh.jclient.common.HttpHeaders;
import ru.hh.jclient.common.Monitoring;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

/**
 * UpstreamMonitoring sends requests metrics to okmeter.io using StatsD.
 * <p>
 * Metrics:
 * - http.client.requests
 * - http.client.request.time
 * - http.client.retries
 */
public class UpstreamMonitoring implements Monitoring {
  private final StatsDSender statsDSender;
  private final String serviceName;

  public UpstreamMonitoring(StatsDSender statsDSender, String serviceName) {
    this.statsDSender = statsDSender;
    this.serviceName = serviceName;
  }

  @Override
  public void countRequest(
      String upstreamName,
      String serverDatacenter,
      String serverAddress,
      HttpHeaders requestHeaders,
      int statusCode,
      long requestTimeMillis,
      boolean isRequestFinal,
      String balancingStrategyType
  ) {
    Map<String, String> tags = getCommonTags(serviceName, upstreamName, serverDatacenter);
    tags.put("status", String.valueOf(statusCode));
    tags.put("final", String.valueOf(isRequestFinal));
    tags.put("balancing", requireNonNullElse(balancingStrategyType, "unknown"));
    statsDSender.sendCount("http.client.requests", 1, toTagsArray(tags));
  }

  @Override
  public void countRequestTime(String upstreamName, String serverDatacenter, HttpHeaders requestHeaders, long requestTimeMillis) {
    Map<String, String> tags = getCommonTags(serviceName, upstreamName, serverDatacenter);
    statsDSender.sendTime("http.client.request.time", requestTimeMillis, toTagsArray(tags));
  }

  @Override
  public void countRetry(
      String upstreamName,
      String serverDatacenter,
      String serverAddress,
      HttpHeaders requestHeaders,
      int statusCode,
      int firstStatusCode,
      int triesUsed
  ) {
    Map<String, String> tags = getCommonTags(serviceName, upstreamName, serverDatacenter);
    tags.put("status", String.valueOf(statusCode));
    tags.put("first_status", String.valueOf(firstStatusCode));
    tags.put("tries", String.valueOf(triesUsed));
    statsDSender.sendCount("http.client.retries", 1, toTagsArray(tags));
  }

  @Override
  public void countUpdateIgnore(String upstreamName, String clientDatacenter) {
    statsDSender.sendCount("http.client.not.ehough.servers.update", 1, toTagsArray(getCommonTags(serviceName, upstreamName, clientDatacenter)));
  }

  private static Map<String, String> getCommonTags(String serviceName, String upstreamName, String datacenter) {
    Map<String, String> tags = new HashMap<>();
    tags.put("app", serviceName);
    tags.put("upstream", upstreamName);
    tags.put("dc", datacenter);
    return tags;
  }

  private static Tag[] toTagsArray(Map<String, String> tags) {
    return tags
        .entrySet()
        .stream()
        .filter(p -> p.getValue() != null)
        .map(p -> new Tag(p.getKey(), p.getValue()))
        .toArray(Tag[]::new);
  }
}
