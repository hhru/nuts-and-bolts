package ru.hh.nab.consul;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ru.hh.consul.HealthClient;
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
            .map(hs -> new HostPort(hs.getActualServiceAddress(), hs.getService().getPort()))
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
}
