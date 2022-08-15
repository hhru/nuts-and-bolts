package ru.hh.nab.starter.consul;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;
import ru.hh.nab.starter.server.jetty.JettySettingsConstants;

public class ConsulService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";
  private static final int DEFAULT_WEIGHT = 100;
  public static final int DEFAULT_WEIGHT_CACHE_WATCH_SECONDS = 10;

  public static final String SERVICE_ADDRESS_PROPERTY = "consul.service.address";
  public static final String AUTO_RESOLVE_ADDRESS_VALUE = "resolve";
  public static final String WAIT_AFTER_DEREGISTRATION_PROPERTY = "consul.wait.after.deregistration.millis";
  public static final String CONSUL_CHECK_HOST_PROPERTY = "consul.check.host";
  public static final String CONSUL_TAGS_PROPERTY = "consul.tags";
  public static final String CONSUL_ENABLED_PROPERTY = "consul.enabled";
  public static final String CONSUL_REGISTRATION_ENABLED_PROPERTY = "consul.registration.enabled";
  public static final String CONSUL_CHECK_INTERVAL_PROPERTY = "consul.check.interval";
  public static final String CONSUL_CHECK_TIMEOUT_PROPERTY = "consul.check.timeout";
  public static final String CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY = "consul.deregisterCritical.timeout";
  public static final String CONSUL_CHECK_SUCCESS_COUNT_PROPERTY = "consul.check.successCount";
  public static final String CONSUL_CHECK_FAIL_COUNT_PROPERTY = "consul.check.failCount";
  public static final String CONSUL_CHECK_PASSING = "consul.check.passing";
  public static final String CONSUL_COMMON_CONSISTENCY_MODE_PROPERTY = "consul.consistencyMode";
  public static final String CONSUL_WEIGHT_CACHE_WATCH_INTERVAL_PROPERTY = "consul.weightCache.watchSeconds";
  public static final String CONSUL_WEIGHT_CACHE_CONSISTENCY_MODE_PROPERTY = "consul.weightCache.consistencyMode";

  private final AgentClient agentClient;
  private final KeyValueClient kvClient;
  private final ImmutableRegistration serviceTemplate;

  private final String serviceName;
  private final String serviceId;
  private final String hostName;
  private final boolean registrationEnabled;
  private final String weightPath;
  private final int sleepAfterDeregisterMillis;
  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;

  private final AtomicReference<Integer> weight = new AtomicReference<>(null);
  private volatile KVCache kvCache;

  public ConsulService(AgentClient agentClient, KeyValueClient kvClient,
                       FileSettings fileSettings, AppMetadata appMetadata,
                       @Nullable LogLevelOverrideExtension logLevelOverrideExtension) {
    int applicationPort = Integer.parseInt(fileSettings.getNotEmptyOrThrow(JettySettingsConstants.JETTY_PORT));
    this.hostName = fileSettings.getNotEmptyOrThrow(NODE_NAME);
    this.serviceName = fileSettings.getNotEmptyOrThrow(SERVICE_NAME);
    this.serviceId = serviceName + '-' + this.hostName + '-' + applicationPort;
    this.agentClient = agentClient;
    this.kvClient = kvClient;
    this.weightPath = String.format("host/%s/weight", this.hostName);
    String resultingConsistencyMode = fileSettings.getString(
      CONSUL_WEIGHT_CACHE_CONSISTENCY_MODE_PROPERTY,
      fileSettings.getString(CONSUL_COMMON_CONSISTENCY_MODE_PROPERTY, ConsistencyMode.DEFAULT.name())
    );
    this.consistencyMode = Stream.of(ConsistencyMode.values())
      .filter(mode -> mode.name().equalsIgnoreCase(resultingConsistencyMode))
      .findAny()
      .orElse(ConsistencyMode.DEFAULT);
    this.watchSeconds = fileSettings.getInteger(CONSUL_WEIGHT_CACHE_WATCH_INTERVAL_PROPERTY, DEFAULT_WEIGHT_CACHE_WATCH_SECONDS);
    this.sleepAfterDeregisterMillis = fileSettings.getInteger(WAIT_AFTER_DEREGISTRATION_PROPERTY, 300);
    Optional<String> address = resolveAddress(fileSettings);
    var applicationHost = fileSettings.getString(CONSUL_CHECK_HOST_PROPERTY, address.orElse("127.0.0.1"));

    var tags = new ArrayList<>(fileSettings.getStringList(CONSUL_TAGS_PROPERTY));
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }
    this.registrationEnabled = fileSettings.getBoolean(CONSUL_REGISTRATION_ENABLED_PROPERTY, fileSettings.getBoolean(CONSUL_ENABLED_PROPERTY, true));
    if (registrationEnabled) {
      Registration.RegCheck regCheck = ImmutableRegCheck.builder()
        .http("http://" + applicationHost + ":" + applicationPort + "/status")
        .status(Optional.ofNullable(fileSettings.getBoolean(CONSUL_CHECK_PASSING)).filter(Boolean.TRUE::equals).map(ignored -> "passing"))
        .interval(fileSettings.getString(CONSUL_CHECK_INTERVAL_PROPERTY, "5s"))
        .timeout(fileSettings.getString(CONSUL_CHECK_TIMEOUT_PROPERTY, "5s"))
        .deregisterCriticalServiceAfter(fileSettings.getString(CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY, "10m"))
        .successBeforePassing(fileSettings.getInteger(CONSUL_CHECK_SUCCESS_COUNT_PROPERTY, 1))
        .failuresBeforeCritical(fileSettings.getInteger(CONSUL_CHECK_FAIL_COUNT_PROPERTY, 1))
        .build();

      this.serviceTemplate = ImmutableRegistration.builder()
        .id(serviceId)
        .name(fileSettings.getString(SERVICE_NAME))
        .port(applicationPort)
        .address(address)
        .check(regCheck)
        .tags(tags)
        .meta(Map.of("serviceVersion", appMetadata.getVersion()))
        .build();
    } else {
      this.serviceTemplate = null;
    }
  }

  public void register() {
    if (!registrationEnabled) {
      LOGGER.info("Registration disabled. Skipping");
      return;
    }
    try {
      LOGGER.debug("Starting registration");
      Optional<Map.Entry<BigInteger, Optional<String>>> currentIndexAndWeight = getCurrentWeight();
      this.weight.set(getWeightOrDefault(currentIndexAndWeight.flatMap(Map.Entry::getValue)));
      this.kvCache = KVCache.newCache(this.kvClient, weightPath, watchSeconds, currentIndexAndWeight.map(Map.Entry::getKey).orElse(null),
        ImmutableQueryOptions.builder().consistencyMode(consistencyMode).caller(serviceName).build()
      );
      LOGGER.trace("Got current weight for service: {}", weight.get());
      var registration = registerWithWeight(weight.get());
      LOGGER.trace("Registered service, starting cache to watch weight change");
      startCache();
      LOGGER.info("Registered consul service: {} and started cache to track weight changes", registration);
    } catch (RuntimeException ex) {
      throw new ConsulServiceException("Can't register service in consul", ex);
    }
  }

  private ImmutableRegistration registerWithWeight(int weight) {
    ServiceWeights serviceWeights = ImmutableServiceWeights.builder().passing(weight).warning(0).build();
    ImmutableRegistration registration = ImmutableRegistration.copyOf(serviceTemplate).withServiceWeights(serviceWeights);
    agentClient.register(registration, ImmutableQueryOptions.builder().caller(serviceName).build());
    return registration;
  }

  private Integer getWeightOrDefault(Optional<String> maybeWeight){
    return maybeWeight.map(Integer::valueOf).orElseGet(() -> {
      LOGGER.info("No weight present for node:{}. Setting default value = {}", hostName, DEFAULT_WEIGHT);
      return DEFAULT_WEIGHT;
    });
  }

  private Optional<Map.Entry<BigInteger, Optional<String>>> getCurrentWeight() {
    return kvClient.getConsulResponseWithValue(weightPath, ImmutableQueryOptions.builder().caller(serviceName).build())
      .map(response -> Map.entry(response.getIndex(), response.getResponse().getValueAsString()));
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
    Optional.ofNullable(kvCache).ifPresent(KVCache::stop);
    agentClient.deregister(serviceId, ImmutableQueryOptions.builder().caller(serviceName).build());
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

  private static Optional<String> resolveAddress(FileSettings fileSettings) {
    return Optional.ofNullable(fileSettings.getString(SERVICE_ADDRESS_PROPERTY)).map(addressValue -> {
      if (AUTO_RESOLVE_ADDRESS_VALUE.equalsIgnoreCase(addressValue)) {
        try {
          return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
          throw new IllegalStateException(
              SERVICE_ADDRESS_PROPERTY + " is set to " + AUTO_RESOLVE_ADDRESS_VALUE + ", but failed to resolve address", e
          );
        }
      }
      return addressValue;
    });
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
