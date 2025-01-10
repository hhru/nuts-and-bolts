package ru.hh.nab.consul;

import java.util.Set;

public interface HostsFetcher {

  Set<HostPort> fetchHostsByName(String serviceName);

}
