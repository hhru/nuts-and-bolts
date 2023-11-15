package ru.hh.nab.starter.consul;


import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.consul.HealthClient;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.health.ImmutableNode;
import ru.hh.consul.model.health.ImmutableService;
import ru.hh.consul.model.health.ImmutableServiceHealth;
import ru.hh.consul.model.health.ServiceHealth;
import ru.hh.nab.common.properties.FileSettings;

class ConsulFetcherTest {

  @Test
  void testFetchHostsByName() {
    HealthClient healthClient = mock(HealthClient.class);
    FileSettings fileSettings = mock(FileSettings.class);
    ConsulFetcher consulFetcher = new ConsulFetcher(healthClient, fileSettings, "service");

    List<ServiceHealth> response = List.of(
        ImmutableServiceHealth
            .builder()
            .node(ImmutableNode.builder().node("node").address("127.0.0.1").datacenter("dc1").build())
            .service(ImmutableService.builder().service("scylla").address("11.11.11.11").id("1").port(1488).build())
            .build(),
        ImmutableServiceHealth
            .builder()
            .node(ImmutableNode.builder().node("node").address("127.0.0.1").datacenter("dc2").build())
            .service(ImmutableService.builder().service("scylla").address("22.22.22.22").id("2").port(1488).build())
            .build(),
        ImmutableServiceHealth
            .builder()
            .node(ImmutableNode.builder().node("node").address("127.0.0.1").datacenter("dc3").build())
            .service(ImmutableService.builder().service("scylla").address("33.33.33.33").id("3").port(1488).build())
            .build()
    );
    Properties properties = new Properties();
    properties.put("datacenters", "dc1, dc2, dc3");

    ConsulResponse<List<ServiceHealth>> healthClientResponse = new ConsulResponse<>(response, 0, false, BigInteger.ONE, Optional.empty());
    when(healthClient.getHealthyServiceInstances(Mockito.anyString(), Mockito.any())).thenReturn(healthClientResponse);
    when(fileSettings.getProperties()).thenReturn(properties);

    Set<HostPort> hostPorts = consulFetcher.fetchHostsByName("scylla");

    assertEquals(3, hostPorts.size());
  }
}
