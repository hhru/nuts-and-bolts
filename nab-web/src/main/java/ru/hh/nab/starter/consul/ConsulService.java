package ru.hh.nab.starter.consul;

import jakarta.annotation.Nullable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import static ru.hh.nab.starter.consul.ConsulProperties.CONSUL_SERVICE_ADDRESS_PROPERTY;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

public class ConsulService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";
  private static final int DEFAULT_WEIGHT = 100;

  public static final String AUTO_RESOLVE_ADDRESS_VALUE = "resolve";

  private final AgentClient agentClient;
  private final KeyValueClient kvClient;
  private final ImmutableRegistration serviceTemplate;

  private final String serviceName;
  private final String serviceId;
  private final String nodeName;
  private final boolean registrationEnabled;
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
      ConsulProperties consulProperties,
      @Nullable LogLevelOverrideExtension logLevelOverrideExtension
  ) {
    this.nodeName = nodeName;
    this.serviceName = serviceName;
    this.serviceId = serviceName + '-' + nodeName + '-' + applicationPort;
    this.agentClient = agentClient;
    this.kvClient = kvClient;
    this.weightPath = String.format("host/%s/weight", nodeName);
    String resultingConsistencyMode = Optional
        .ofNullable(consulProperties.getWeightCache().getConsistencyMode())
        .orElseGet(consulProperties::getConsistencyMode);
    this.consistencyMode = Stream
        .of(ConsistencyMode.values())
        .filter(mode -> mode.name().equalsIgnoreCase(resultingConsistencyMode))
        .findAny()
        .orElse(ConsistencyMode.DEFAULT);
    this.watchSeconds = consulProperties.getWeightCache().getWatchSeconds();
    this.sleepAfterDeregisterMillis = consulProperties.getWaitAfterDeregistrationMillis();
    Optional<String> address = resolveAddress(consulProperties.getService().getAddress());
    var applicationHost = Optional
        .ofNullable(consulProperties.getCheck().getHost())
        .or(() -> address)
        .orElse("127.0.0.1");

    var tags = new ArrayList<>(consulProperties.getTags());
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }
    this.registrationEnabled = consulProperties.getRegistration().isEnabled();
    if (registrationEnabled) {
      Registration.RegCheck regCheck = ImmutableRegCheck
          .builder()
          .http("http://" + applicationHost + ":" + applicationPort + "/status")
          .status(Optional.ofNullable(consulProperties.getCheck().getPassing()).filter(Boolean.TRUE::equals).map(ignored -> "passing"))
          .interval(consulProperties.getCheck().getInterval())
          .timeout(consulProperties.getCheck().getTimeout())
          .deregisterCriticalServiceAfter(consulProperties.getDeregisterCritical().getTimeout())
          .successBeforePassing(consulProperties.getCheck().getSuccessCount())
          .failuresBeforeCritical(consulProperties.getCheck().getFailCount())
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

  private static Optional<String> resolveAddress(String serviceAddress) {
    return Optional
        .ofNullable(serviceAddress)
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
