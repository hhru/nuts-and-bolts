package ru.hh.nab.starter;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import static java.util.Objects.requireNonNullElse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class ConsulService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";

  private final AgentClient agentClient;
  private final Registration service;
  private final String id;
  private final boolean enabled;

  public ConsulService(AgentClient agentClient, FileSettings fileSettings, String datacenter, String host, AppMetadata appMetadata,
                       @Nullable LogLevelOverrideExtension logLevelOverrideExtension) {
    this.agentClient = agentClient;
    var applicationPort = fileSettings.getInteger("jetty.port");
    var applicationHost = Optional.ofNullable(fileSettings.getString("consul.check.host"))
      .orElse("127.0.0.1");
    var id = fileSettings.getString("serviceName") + "-" + datacenter + "-" + host + "-" + applicationPort;
    var tags = new ArrayList<>(fileSettings.getStringList("consul.tags"));
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }

    ImmutableRegCheck regCheck = ImmutableRegCheck.builder()
            .http("http://" + applicationHost + ":" + applicationPort + "/status")
            .interval(requireNonNullElse(fileSettings.getString("consul.check.interval"), "5s"))
            .timeout(requireNonNullElse(fileSettings.getString("consul.check.timeout"), "5s"))
            .build();

    this.service = ImmutableRegistration.builder()
            .id(id)
            .name(fileSettings.getString("serviceName"))
            .port(applicationPort)
            .check(regCheck)
            .tags(tags)
            .meta(Collections.singletonMap("serviceVersion", appMetadata.getVersion()))
            .build();

    this.id = id;
    this.enabled = Optional.ofNullable(fileSettings.getBoolean("consul.enabled")).orElse(true);
  }

  public void register() {
    if (enabled) {
      try {
        agentClient.register(service);
        LOGGER.info("Registered consul service: {}", service);
      } catch (RuntimeException ex) {
        throw new ConsulServiceException("Can't register service in consul", ex);
      }
    }
  }

  @PreDestroy
  void deregister() {
    if (enabled) {
      agentClient.deregister(service.getId());
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
      "client=" + agentClient +
      ", service=" + service +
      ", id='" + id + '\'' +
      '}';
  }
}
