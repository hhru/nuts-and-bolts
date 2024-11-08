package ru.hh.nab.web.consul;

import java.util.Set;

public interface HostsFetcher {

  Set<HostPort> fetchHostsByName(String serviceName);

}
