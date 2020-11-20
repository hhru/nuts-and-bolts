package ru.hh.nab.starter;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.ImmutableServiceWeights;
import com.orbitz.consul.model.catalog.ServiceWeights;
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
  private static final int DEFAULT_WEIGHT = 100;

  private final AgentClient agentClient;
  private final KeyValueClient kvClient;
  private final Registration service;
  private final String id;
  private final boolean enabled;

  public ConsulService(AgentClient agentClient, KeyValueClient kvClient,
                       FileSettings fileSettings, String hostName, AppMetadata appMetadata,
                       @Nullable LogLevelOverrideExtension logLevelOverrideExtension) {
    this.agentClient = agentClient;
    this.kvClient = kvClient;

    var applicationPort = fileSettings.getInteger("jetty.port");
    var applicationHost = Optional.ofNullable(fileSettings.getString("consul.check.host"))
        .orElse("127.0.0.1");
    var id = fileSettings.getString("serviceName") + "-" + hostName + "-" + applicationPort;
    var tags = new ArrayList<>(fileSettings.getStringList("consul.tags"));
    var warningDivider = fileSettings.getInteger("consul.check.warningDivider", 3);
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }

    Registration.RegCheck regCheck = ImmutableRegCheck.builder()
        .http("http://" + applicationHost + ":" + applicationPort + "/status")
        .interval(fileSettings.getString("consul.check.interval", "5s"))
        .timeout(fileSettings.getString("consul.check.timeout", "5s"))
        .deregisterCriticalServiceAfter(fileSettings.getString("consul.deregisterCritical.timeout", "10m"))
        .successBeforePassing(fileSettings.getInteger("consul.check.successCount", 2))
        .failuresBeforeCritical(fileSettings.getInteger("consul.check.failCount", 2))
        .build();

    Optional<String> weight = getWeight(hostName);
    int weightForService;
    if (weight.isPresent()) {
      weightForService = weight.map(Integer::parseInt).get();
    } else {
      LOGGER.warn("No weight present for node:{}", hostName);
      weightForService = DEFAULT_WEIGHT;
    }

    ServiceWeights serviceWeights = ImmutableServiceWeights.builder().passing(weightForService).warning(weightForService / warningDivider).build();

    this.service = ImmutableRegistration.builder()
        .id(id)
        .name(fileSettings.getString("serviceName"))
        .port(applicationPort)
        .check(regCheck)
        .serviceWeights(serviceWeights)
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

  Optional<String> getWeight(String hostName){
    return kvClient.getValueAsString(String.format("host/%s/weight", hostName));
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
