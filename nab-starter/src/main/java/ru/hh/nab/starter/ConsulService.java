package ru.hh.nab.starter;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class ConsulService {

  private static final Logger logger = LoggerFactory.getLogger(ConsulService.class);

  private final ConsulClient client;
  private final NewService service;
  private final String id;
  private final boolean enabled;

  public ConsulService(FileSettings fileSettings, String datacenter, String address, AppMetadata appMetadata) {
    var port = fileSettings.getInteger("jetty.port");
    var id = fileSettings.getString("serviceName") + "-" + datacenter + "-" + address;
    var tags = fileSettings.getStringList("consul.tags");

    NewService.Check check = new NewService.Check();
    check.setHttp("http://localhost:" + port + "/status");
    check.setTimeout(fileSettings.getString("consul.check.timeout"));
    check.setInterval(fileSettings.getString("consul.check.interval"));
    check.setMethod("GET");

    NewService service = new NewService();
    service.setId(id);
    service.setName(fileSettings.getString("serviceName"));
    service.setPort(port);
    service.setAddress(address);
    service.setCheck(check);
    service.setTags(tags);
    service.setMeta(Collections.singletonMap("serviceVersion", appMetadata.getVersion()));

    this.client = new ConsulClient("localhost", fileSettings.getInteger("consul.http.port"));
    this.service = service;
    this.id = id;
    this.enabled = Optional.ofNullable(fileSettings.getBoolean("consul.enabled")).orElse(true);
  }

  public ConsulClient getClient() {
    return client;
  }

  @PostConstruct
  void register() {
    if (enabled) {
      client.agentServiceRegister(service);
      logger.info("Registered service: {} to consul", service);
    }
  }

  @PreDestroy
  void deregister() {
    if (enabled) {
      client.agentServiceDeregister(id);
      logger.info("De-registered id: {} from consul", id);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsulService that = (ConsulService) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "ConsulService{" +
      "client=" + client +
      ", service=" + service +
      ", id='" + id + '\'' +
      '}';
  }
}
