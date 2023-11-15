package ru.hh.nab.starter.consul;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import ru.hh.consul.HealthClient;
import ru.hh.consul.model.health.ServiceHealth;
import ru.hh.consul.option.ImmutableQueryOptions;
import ru.hh.consul.option.QueryOptions;
import ru.hh.nab.common.properties.FileSettings;

public class ConsulFetcher implements HostsFetcher {
  private static final String DATACENTERS = "datacenters";
  private final HealthClient healthClient;
  private final FileSettings settings;
  private final String serviceName;

  public ConsulFetcher(HealthClient healthClient, FileSettings settings, String serviceName) {
    this.healthClient = healthClient;
    this.settings = settings;
    this.serviceName = serviceName;
  }

  @Override
  public Set<HostPort> fetchHostsByName(String serviceName) {
    return Optional
        .ofNullable(settings.getProperties().getProperty(DATACENTERS))
        .filter(Predicate.not(String::isBlank))
        .map(separatedDcList -> List.of(separatedDcList.split("[,\\s]+")))
        .orElseGet(List::of)
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
