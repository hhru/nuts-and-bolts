package ru.hh.nab.starter.consul;

import java.util.Set;

public interface HostsFetcher {

  Set<HostPort> fetchHostsByName(String serviceName);

}
