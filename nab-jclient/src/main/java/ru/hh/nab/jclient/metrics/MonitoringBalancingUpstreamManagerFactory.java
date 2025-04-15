package ru.hh.nab.jclient.metrics;

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

public class MonitoringBalancingUpstreamManagerFactory {

  public static BalancingUpstreamManager createWithDefaults(
      JClientInfrastructureConfig infrastructureConfig,
      ConfigStore configStore,
      ServerStore serverStore,
      Properties strategyProperties,
      Set<Monitoring> monitoring
  ) {
    boolean allowCrossDCRequests = Optional
        .ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_KEY))
        .or(() -> Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_PATH)))
        .map(Boolean::parseBoolean)
        .orElse(false);
    return new BalancingUpstreamManager(
        configStore,
        serverStore,
        monitoring,
        infrastructureConfig,
        allowCrossDCRequests
    );
  }
}
