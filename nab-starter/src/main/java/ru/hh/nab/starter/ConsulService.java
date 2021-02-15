package ru.hh.nab.starter;

import ru.hh.consul.AgentClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.cache.KVCache;
import ru.hh.consul.model.agent.ImmutableRegCheck;
import ru.hh.consul.model.agent.ImmutableRegistration;
import ru.hh.consul.model.agent.Registration;
import ru.hh.consul.model.catalog.ImmutableServiceWeights;
import ru.hh.consul.model.catalog.ServiceWeights;
import ru.hh.consul.model.kv.Value;
import ru.hh.consul.option.ConsistencyMode;
import ru.hh.consul.option.ImmutableQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;
import ru.hh.nab.starter.server.jetty.JettySettingsConstants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ConsulService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";
  private static final int DEFAULT_WEIGHT = 100;
  public static final int DEFAULT_WEIGHT_CACHE_WATCH_SECONDS = 10;

  public static final String SERVICE_ADDRESS_PROPERTY = "consul.service.address";
  public static final String WAIT_AFTER_DEREGISTRATION_PROPERTY = "consul.wait.after.deregistration.millis";
  public static final String WARNING_DIVIDER_PROPERTY = "consul.check.warningDivider";
  public static final String CONSUL_CHECK_HOST_PROPERTY = "consul.check.host";
  public static final String CONSUL_TAGS_PROPERTY = "consul.tags";
  public static final String CONSUL_ENABLED_PROPERTY = "consul.enabled";
  public static final String CONSUL_REGISTRATION_ENABLED_PROPERTY = "consul.registration.enabled";
  public static final String CONSUL_CHECK_INTERVAL_PROPERTY = "consul.check.interval";
  public static final String CONSUL_CHECK_TIMEOUT_PROPERTY = "consul.check.timeout";
  public static final String CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY = "consul.deregisterCritical.timeout";
  public static final String CONSUL_CHECK_SUCCESS_COUNT_PROPERTY = "consul.check.successCount";
  public static final String CONSUL_CHECK_FAIL_COUNT_PROPERTY = "consul.check.failCount";
  public static final String CONSUL_COMMON_CONSISTENCY_MODE_PROPERTY = "consul.consistencyMode";
  public static final String CONSUL_WEIGHT_CACHE_WATCH_INTERVAL_PROPERTY = "consul.weightCache.watchSeconds";
  public static final String CONSUL_WEIGHT_CACHE_CONSISTENCY_MODE_PROPERTY = "consul.weightCache.consistencyMode";

  private final AgentClient agentClient;
  private final KeyValueClient kvClient;
  private final Supplier<ImmutableRegistration.Builder> serviceTemplate;
  private final KVCache kvCache;

  private final String serviceId;
  private final String hostName;
  private final boolean registrationEnabled;
  private final String weightPath;
  private final int warningDivider;
  private final long sleepAfterDeregisterMillis;
  private final AtomicReference<Integer> weight = new AtomicReference<>(null);

  public ConsulService(AgentClient agentClient, KeyValueClient kvClient,
                       FileSettings fileSettings, AppMetadata appMetadata,
                       @Nullable LogLevelOverrideExtension logLevelOverrideExtension) {
    int applicationPort = Integer.parseInt(fileSettings.getNotEmptyOrThrow(JettySettingsConstants.JETTY_PORT));
    this.hostName = fileSettings.getNotEmptyOrThrow(NabCommonConfig.NODE_NAME_PROPERTY);
    this.serviceId = fileSettings.getNotEmptyOrThrow(NabCommonConfig.SERVICE_NAME_PROPERTY) + "-" + this.hostName + "-" + applicationPort;
    this.agentClient = agentClient;
    this.kvClient = kvClient;
    this.weightPath = String.format("host/%s/weight", this.hostName);
    String resultingConsistencyMode = fileSettings.getString(
      CONSUL_WEIGHT_CACHE_CONSISTENCY_MODE_PROPERTY,
      fileSettings.getString(CONSUL_COMMON_CONSISTENCY_MODE_PROPERTY, ConsistencyMode.DEFAULT.name())
    );
    var consistencyMode = Stream.of(ConsistencyMode.values())
      .filter(mode -> mode.name().equalsIgnoreCase(resultingConsistencyMode))
      .findAny()
      .orElse(ConsistencyMode.DEFAULT);

    int watchSeconds = fileSettings.getInteger(CONSUL_WEIGHT_CACHE_WATCH_INTERVAL_PROPERTY, DEFAULT_WEIGHT_CACHE_WATCH_SECONDS);
    this.kvCache = KVCache.newCache(this.kvClient, weightPath, watchSeconds,
        ImmutableQueryOptions.builder().consistencyMode(consistencyMode).build()
    );
    this.sleepAfterDeregisterMillis = fileSettings.getLong(WAIT_AFTER_DEREGISTRATION_PROPERTY, 300L);

    this.warningDivider = fileSettings.getInteger(WARNING_DIVIDER_PROPERTY, 3);
    var applicationHost = fileSettings.getString(CONSUL_CHECK_HOST_PROPERTY, "127.0.0.1");

    var tags = new ArrayList<>(fileSettings.getStringList(CONSUL_TAGS_PROPERTY));
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }
    this.registrationEnabled = fileSettings.getBoolean(CONSUL_REGISTRATION_ENABLED_PROPERTY, fileSettings.getBoolean(CONSUL_ENABLED_PROPERTY, true));
    if (registrationEnabled) {
      Registration.RegCheck regCheck = ImmutableRegCheck.builder()
        .http("http://" + applicationHost + ":" + applicationPort + "/status")
        .interval(fileSettings.getString(CONSUL_CHECK_INTERVAL_PROPERTY, "5s"))
        .timeout(fileSettings.getString(CONSUL_CHECK_TIMEOUT_PROPERTY, "5s"))
        .deregisterCriticalServiceAfter(fileSettings.getString(CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY, "10m"))
        .successBeforePassing(fileSettings.getInteger(CONSUL_CHECK_SUCCESS_COUNT_PROPERTY, 1))
        .failuresBeforeCritical(fileSettings.getInteger(CONSUL_CHECK_FAIL_COUNT_PROPERTY, 1))
        .build();

      this.serviceTemplate = () -> ImmutableRegistration.builder()
        .id(serviceId)
        .name(fileSettings.getString(NabCommonConfig.SERVICE_NAME_PROPERTY))
        .port(applicationPort)
        .address(Optional.ofNullable(fileSettings.getString(SERVICE_ADDRESS_PROPERTY)))
        .check(regCheck)
        .tags(tags)
        .meta(Map.of("serviceVersion", appMetadata.getVersion()));
    } else {
      this.serviceTemplate = () -> {
        throw new IllegalStateException("Registration disabled. Template should not be called");
      };
    }
  }

  public void register() {
    if (!registrationEnabled) {
      LOGGER.info("Registration disabled. Skipping");
      return;
    }
    try {
      this.weight.set(getWeightOrDefault(getCurrentWeight()));
      var registration = registerWithWeight(weight.get());
      startCache();
      LOGGER.info("Registered consul service: {} and started cache to track weight changes", registration);
    } catch (RuntimeException ex) {
      throw new ConsulServiceException("Can't register service in consul", ex);
    }
  }

  private ImmutableRegistration registerWithWeight(int weight) {
    ServiceWeights serviceWeights = ImmutableServiceWeights.builder().passing(weight).warning(weight / warningDivider).build();
    ImmutableRegistration registration = serviceTemplate.get().serviceWeights(serviceWeights).build();
    agentClient.register(registration);
    return registration;
  }

  private Integer getWeightOrDefault(Optional<String> maybeWeight){
    return maybeWeight.map(Integer::valueOf).orElseGet(() -> {
      LOGGER.info("No weight present for node:{}. Setting default value = {}", hostName, DEFAULT_WEIGHT);
      return DEFAULT_WEIGHT;
    });
  }

  private Optional<String> getCurrentWeight() {
    return kvClient.getValueAsString(weightPath);
  }

  private void startCache() {
    kvCache.addListener(newValues -> {
      var newWeight = getWeightOrDefault(newValues.values().stream().findAny().flatMap(Value::getValueAsString));
      var oldWeight = weight.get();
      if (!Objects.equals(oldWeight, newWeight) && weight.compareAndSet(oldWeight, newWeight)) {
        var registration = registerWithWeight(newWeight);
        LOGGER.info("Updated registration for consul service: {}", registration);
      }
    });
    kvCache.start();
  }

  public void deregister() {
    if (!registrationEnabled) {
      LOGGER.info("Registration disabled. Skipping deregistration");
      return;
    }
    kvCache.stop();
    agentClient.deregister(serviceId);
    LOGGER.debug("De-registered id: {} from consul, going to sleep {}ms to wait possible requests", serviceId, sleepAfterDeregisterMillis);
    sleepAfterDeregistration();
    LOGGER.info("De-registered id: {} from consul", serviceId);
  }

  private void sleepAfterDeregistration() {
    try {
      Thread.sleep(sleepAfterDeregisterMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
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
    return Objects.equals(serviceId, that.serviceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId);
  }

  @Override
  public String toString() {
    return "ConsulService{" +
      "client=" + agentClient +
      ", service=" + serviceTemplate +
      ", serviceId='" + serviceId + '\'' +
      '}';
  }
}
