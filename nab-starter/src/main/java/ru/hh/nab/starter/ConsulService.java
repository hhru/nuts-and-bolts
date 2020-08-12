package ru.hh.nab.starter;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import java.util.ArrayList;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.exceptions.ConsulServiceException;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

public class ConsulService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";

  private final ConsulClient client;
  private final NewService service;
  private final String id;
  private final boolean enabled;

  public ConsulService(FileSettings fileSettings, String datacenter, String address, AppMetadata appMetadata,
                       @Nullable LogLevelOverrideExtension logLevelOverrideExtension) {
    var applicationPort = fileSettings.getInteger("jetty.port");
    var applicationHost = Optional.ofNullable(fileSettings.getString("consul.check.host"))
      .orElse("127.0.0.1");
    var id = fileSettings.getString("serviceName") + "-" + datacenter + "-" + address + "-" + applicationPort;
    var tags = new ArrayList<>(fileSettings.getStringList("consul.tags"));
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }

    NewService.Check check = new NewService.Check();
    check.setHttp("http://" + applicationHost + ":" + applicationPort + "/status");
    check.setTimeout(fileSettings.getString("consul.check.timeout"));
    check.setInterval(fileSettings.getString("consul.check.interval"));
    check.setMethod("GET");
    check.setStatus("passing");

    NewService service = new NewService();
    service.setId(id);
    service.setName(fileSettings.getString("serviceName"));
    service.setPort(applicationPort);
    service.setAddress(address);
    service.setCheck(check);
    service.setTags(tags);
    service.setMeta(Collections.singletonMap("serviceVersion", appMetadata.getVersion()));

    this.client = new ConsulClient(
      Optional.ofNullable(fileSettings.getString("consul.http.host")).orElse("127.0.0.1"),
      fileSettings.getInteger("consul.http.port")
    );
    this.service = service;
    this.id = id;
    this.enabled = Optional.ofNullable(fileSettings.getBoolean("consul.enabled")).orElse(true);
  }

  public ConsulClient getClient() {
    return client;
  }

  public void register() {
    if (enabled) {
      try {
        client.agentServiceRegister(service);
        LOGGER.info("Registered consul service: {}", service);
      } catch (RuntimeException ex) {
        throw new ConsulServiceException("Can't register service in consul", ex);
      }
    }
  }

  @PreDestroy
  void deregister() {
    if (enabled) {
      client.agentServiceDeregister(id);
      LOGGER.info("De-registered id: {} from consul", id);
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
