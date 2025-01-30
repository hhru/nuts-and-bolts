package ru.hh.nab.consul;

import jakarta.annotation.Nullable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
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
import ru.hh.nab.common.properties.PropertiesUtils;

public class ConsulService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  public static final String CONSUL_PROPERTIES_PREFIX = "consul";
  public static final String CONSUL_SERVICE_ADDRESS_PROPERTY = "consul.service.address";
  public static final String CONSUL_WAIT_AFTER_DEREGISTRATION_MILLIS_PROPERTY = "consul.wait.after.deregistration.millis";
  public static final String CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY = "consul.deregisterCritical.timeout";
  public static final String CONSUL_CHECK_HOST_PROPERTY = "consul.check.host";
  public static final String CONSUL_CHECK_INTERVAL_PROPERTY = "consul.check.interval";
  public static final String CONSUL_CHECK_TIMEOUT_PROPERTY = "consul.check.timeout";
  public static final String CONSUL_CHECK_SUCCESS_COUNT_PROPERTY = "consul.check.successCount";
  public static final String CONSUL_CHECK_FAIL_COUNT_PROPERTY = "consul.check.failCount";
  public static final String CONSUL_CHECK_PASSING_PROPERTY = "consul.check.passing";
  public static final String CONSUL_WEIGHT_CACHE_WATCH_SECONDS_PROPERTY = "consul.weightCache.watchSeconds";
  public static final String CONSUL_WEIGHT_CACHE_CONSISTENCY_MODE_PROPERTY = "consul.weightCache.consistencyMode";
  public static final String CONSUL_CONSISTENCY_MODE_PROPERTY = "consul.consistencyMode";

  public static final int DEFAULT_WEIGHT = 100;
  public static final int DEFAULT_WEIGHT_CACHE_WATCH_SECONDS = 10;
  public static final int DEFAULT_WAIT_AFTER_DEREGISTRATION_MILLIS = 300;
  public static final String DEFAULT_CHECK_HOST = "127.0.0.1";
  public static final String DEFAULT_CHECK_INTERVAL = "5s";
  public static final String DEFAULT_CHECK_TIMEOUT = "5s";
  public static final String DEFAULT_DEREGISTER_CRITICAL_TIMEOUT = "10m";
  public static final int DEFAULT_CHECK_SUCCESS_COUNT = 1;
  public static final int DEFAULT_CHECK_FAIL_COUNT = 1;
  public static final ConsistencyMode DEFAULT_CONSISTENCY_MODE = ConsistencyMode.DEFAULT;

  public static final String AUTO_RESOLVE_ADDRESS_VALUE = "resolve";

  private final AgentClient agentClient;
  private final KeyValueClient kvClient;
  private final ImmutableRegistration serviceTemplate;

  private final String serviceName;
  private final String serviceId;
  private final String nodeName;
  private final String weightPath;
  private final int sleepAfterDeregisterMillis;
  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;

  private final AtomicReference<Integer> weight = new AtomicReference<>(null);
  private volatile KVCache kvCache;

  public ConsulService(
      AgentClient agentClient,
      KeyValueClient kvClient,
      String serviceName,
      String serviceVersion,
      String nodeName,
      int applicationPort,
      Properties properties,
      Set<String> tags
  ) {
    this.nodeName = nodeName;
    this.serviceName = serviceName;
    this.serviceId = serviceName + '-' + nodeName + '-' + applicationPort;
    this.agentClient = agentClient;
    this.kvClient = kvClient;
    this.weightPath = String.format("host/%s/weight", nodeName);
    String resultingConsistencyMode = properties.getProperty(
        CONSUL_WEIGHT_CACHE_CONSISTENCY_MODE_PROPERTY,
        properties.getProperty(CONSUL_CONSISTENCY_MODE_PROPERTY, DEFAULT_CONSISTENCY_MODE.name())
    );
    this.consistencyMode = Stream
        .of(ConsistencyMode.values())
        .filter(mode -> mode.name().equalsIgnoreCase(resultingConsistencyMode))
        .findAny()
        .orElse(DEFAULT_CONSISTENCY_MODE);
    this.watchSeconds = PropertiesUtils.getInteger(properties, CONSUL_WEIGHT_CACHE_WATCH_SECONDS_PROPERTY, DEFAULT_WEIGHT_CACHE_WATCH_SECONDS);
    this.sleepAfterDeregisterMillis = PropertiesUtils.getInteger(
        properties,
        CONSUL_WAIT_AFTER_DEREGISTRATION_MILLIS_PROPERTY,
        DEFAULT_WAIT_AFTER_DEREGISTRATION_MILLIS
    );
    Optional<String> address = resolveAddress(properties);
    var applicationHost = properties.getProperty(CONSUL_CHECK_HOST_PROPERTY, address.orElse(DEFAULT_CHECK_HOST));

    Registration.RegCheck regCheck = ImmutableRegCheck
        .builder()
        .http("http://" + applicationHost + ":" + applicationPort + "/status")
        .status(
            Optional
                .ofNullable(PropertiesUtils.getBoolean(properties, CONSUL_CHECK_PASSING_PROPERTY))
                .filter(Boolean.TRUE::equals)
                .map(ignored -> "passing")
        )
        .interval(properties.getProperty(CONSUL_CHECK_INTERVAL_PROPERTY, DEFAULT_CHECK_INTERVAL))
        .timeout(properties.getProperty(CONSUL_CHECK_TIMEOUT_PROPERTY, DEFAULT_CHECK_TIMEOUT))
        .deregisterCriticalServiceAfter(properties.getProperty(CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY, DEFAULT_DEREGISTER_CRITICAL_TIMEOUT))
        .successBeforePassing(PropertiesUtils.getInteger(properties, CONSUL_CHECK_SUCCESS_COUNT_PROPERTY, DEFAULT_CHECK_SUCCESS_COUNT))
        .failuresBeforeCritical(PropertiesUtils.getInteger(properties, CONSUL_CHECK_FAIL_COUNT_PROPERTY, DEFAULT_CHECK_FAIL_COUNT))
        .build();

    this.serviceTemplate = ImmutableRegistration
        .builder()
        .id(serviceId)
        .name(serviceName)
        .port(applicationPort)
        .address(address)
        .check(regCheck)
        .tags(tags)
        .meta(Map.of("serviceVersion", serviceVersion))
        .build();
  }

  public void register() {
    try {
      LOGGER.debug("Starting registration");
      Optional<Map.Entry<BigInteger, Optional<String>>> currentIndexAndWeight = getCurrentWeight();
      this.weight.set(getWeightOrDefault(currentIndexAndWeight.flatMap(Map.Entry::getValue).orElse(null)));
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

  private Integer getWeightOrDefault(@Nullable String maybeWeight) {
    if (maybeWeight == null) {
      LOGGER.info("No weight present for node:{}. Setting default value = {}", nodeName, DEFAULT_WEIGHT);
      return DEFAULT_WEIGHT;
    }
    return Integer.valueOf(maybeWeight);
  }

  private Optional<Map.Entry<BigInteger, Optional<String>>> getCurrentWeight() {
    return kvClient
        .getConsulResponseWithValue(weightPath, ImmutableQueryOptions.builder().caller(serviceName).build())
        .map(response -> Map.entry(response.getIndex(), response.getResponse().getValueAsString()));
  }

  private void startCache() {
    kvCache.addListener(newValues -> {
      var newWeight = getWeightOrDefault(newValues.values().stream().findAny().flatMap(Value::getValueAsString).orElse(null));
      var oldWeight = weight.get();
      if (!Objects.equals(oldWeight, newWeight) && weight.compareAndSet(oldWeight, newWeight)) {
        var registration = registerWithWeight(newWeight);
        LOGGER.info("Updated registration for consul service: {}", registration);
      }
    });
    kvCache.start();
  }

  public void deregister() {
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

  private static Optional<String> resolveAddress(Properties properties) {
    return Optional
        .ofNullable(properties.getProperty(CONSUL_SERVICE_ADDRESS_PROPERTY))
        .map(addressValue -> {
          if (AUTO_RESOLVE_ADDRESS_VALUE.equalsIgnoreCase(addressValue)) {
            try {
              return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
              throw new IllegalStateException(
                  CONSUL_SERVICE_ADDRESS_PROPERTY + " is set to " + AUTO_RESOLVE_ADDRESS_VALUE + ", but failed to resolve address", e
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
