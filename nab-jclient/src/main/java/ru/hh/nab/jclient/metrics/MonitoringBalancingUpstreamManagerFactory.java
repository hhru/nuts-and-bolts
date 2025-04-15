package ru.hh.nab.jclient.metrics;

import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_PATH;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.nab.metrics.StatsDSender;

public class MonitoringBalancingUpstreamManagerFactory {

  public static BalancingUpstreamManager createWithDefaults(
      JClientInfrastructureConfig infrastructureConfig,
      StatsDSender statsDSender,
      ConfigStore configStore,
      ServerStore serverStore,
      Properties strategyProperties,
      Set<Monitoring> additionalMonitoring
  ) {
    boolean allowCrossDCRequests = Optional
        .ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_KEY))
        .or(() -> Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_PATH)))
        .map(Boolean::parseBoolean)
        .orElse(false);
    return new BalancingUpstreamManager(
        configStore,
        serverStore,
        buildMonitoring(infrastructureConfig.getServiceName(), statsDSender, additionalMonitoring),
        infrastructureConfig,
        allowCrossDCRequests
    );
  }

  private static Set<Monitoring> buildMonitoring(String serviceName, StatsDSender statsDSender, Set<Monitoring> additionalMonitoring) {
    Set<Monitoring> monitoring = new HashSet<>();
    monitoring.add(new UpstreamMonitoring(statsDSender, serviceName));
    monitoring.addAll(additionalMonitoring);
    return monitoring;
  }
}
