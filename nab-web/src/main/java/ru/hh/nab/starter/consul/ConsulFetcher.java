package ru.hh.nab.starter.consul;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import ru.hh.consul.HealthClient;
import ru.hh.consul.model.health.ServiceHealth;
import ru.hh.consul.option.ImmutableQueryOptions;
import ru.hh.consul.option.QueryOptions;

public class ConsulFetcher implements HostsFetcher {
  private final HealthClient healthClient;
  private final String serviceName;
  private final List<String> datacenters;

  public ConsulFetcher(HealthClient healthClient, String serviceName, List<String> datacenters) {
    this.healthClient = healthClient;
    this.serviceName = serviceName;
    this.datacenters = datacenters;
  }

  @Override
  public Set<HostPort> fetchHostsByName(String serviceName) {
    return datacenters
        .stream()
        .flatMap(dc -> healthClient
            .getHealthyServiceInstances(serviceName, buildQueryOptions(dc))
            .getResponse()
            .stream()
            .map(hs -> new HostPort(getAddress(hs), hs.getService().getPort()))
        )
        .collect(Collectors.toSet());
  }

  private QueryOptions buildQueryOptions(String datacenter) {
    ImmutableQueryOptions.Builder queryOptions = ImmutableQueryOptions
        .builder()
        .datacenter(datacenter.toLowerCase())
        .caller(serviceName);

    return queryOptions.build();
  }

  private String getAddress(ServiceHealth serviceHealth) {
    String address = serviceHealth.getService().getAddress();

    return StringUtils.isBlank(address) ? serviceHealth.getNode().getAddress() : address;
  }
}
