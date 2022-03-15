package ru.hh.nab.neo.starter;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import ru.hh.nab.neo.starter.exceptions.ConsulServiceException;
import ru.hh.nab.neo.starter.logging.LogLevelOverrideExtension;
import ru.hh.nab.neo.starter.props.Consul;
import ru.hh.nab.neo.starter.props.NabProperties;

public class ConsulService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulService.class);

  private static final String LOG_LEVEL_OVERRIDE_EXTENSION_TAG = "log_level_override_extension_enabled";
  private static final int DEFAULT_WEIGHT = 100;

  private static final String AUTO_RESOLVE_ADDRESS_VALUE = "resolve";

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

  public ConsulService(AgentClient agentClient,
                       KeyValueClient kvClient,
                       NabProperties nabProperties,
                       int applicationPort,
                       AppMetadata appMetadata,
                       @Nullable LogLevelOverrideExtension logLevelOverrideExtension) {;
    Consul consulProperties = nabProperties.getConsul();
    this.hostName = nabProperties.getNodeName();
    this.serviceName = nabProperties.getServiceName();
    this.serviceId = serviceName + '-' + this.hostName + '-' + applicationPort;
    this.agentClient = agentClient;
    this.kvClient = kvClient;
    this.weightPath = String.format("host/%s/weight", this.hostName);
    String resultingConsistencyMode = Optional.ofNullable(consulProperties.getWeightCache().getConsistencyMode())
        .orElse(consulProperties.getConsistencyMode());
    this.consistencyMode = Stream.of(ConsistencyMode.values())
        .filter(mode -> mode.name().equalsIgnoreCase(resultingConsistencyMode))
        .findAny()
        .orElse(ConsistencyMode.DEFAULT);
    this.watchSeconds = consulProperties.getWeightCache().getWatchSeconds();
    this.sleepAfterDeregisterMillis = consulProperties.getWaitAfterDeregistrationMillis();
    Optional<String> address = resolveAddress(consulProperties.getServiceAddress());
    var applicationHost = consulProperties.getCheck().getHost();;

    var tags = consulProperties.getTags();
    if (logLevelOverrideExtension != null) {
      tags.add(LOG_LEVEL_OVERRIDE_EXTENSION_TAG);
    }
    this.registrationEnabled = consulProperties.getRegistrationEnabled();
    if (registrationEnabled) {
      Registration.RegCheck regCheck = ImmutableRegCheck.builder()
          .http("http://" + applicationHost + ":" + applicationPort + "/status")
          .status(Optional.ofNullable(consulProperties.getCheck().getPassing()).filter(Boolean.TRUE::equals).map(ignored -> "passing"))
          .interval(consulProperties.getCheck().getInterval())
          .timeout(consulProperties.getCheck().getTimeout())
          .deregisterCriticalServiceAfter(consulProperties.getDeregisterCriticalTimeout())
          .successBeforePassing(consulProperties.getCheck().getSuccessCount())
          .failuresBeforeCritical(consulProperties.getCheck().getFailCount())
          .build();

      this.serviceTemplate = ImmutableRegistration.builder()
          .id(serviceId)
          .name(this.serviceName)
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

  private static Optional<String> resolveAddress(String serviceAddress) {
    return Optional.ofNullable(serviceAddress).map(addressValue -> {
      if (AUTO_RESOLVE_ADDRESS_VALUE.equalsIgnoreCase(addressValue)) {
        try {
          return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
          throw new IllegalStateException(
              "nab.consul.service.address is set to " + AUTO_RESOLVE_ADDRESS_VALUE + ", but failed to resolve address", e
          );
        }
      }
      return addressValue;
    });
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

  private Integer getWeightOrDefault(Optional<String> maybeWeight) {
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
