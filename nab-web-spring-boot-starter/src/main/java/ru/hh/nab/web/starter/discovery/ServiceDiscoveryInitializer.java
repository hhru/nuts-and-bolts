package ru.hh.nab.web.starter.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import ru.hh.nab.consul.ConsulService;

public class ServiceDiscoveryInitializer {
  private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryInitializer.class);

  private final ConsulService consulService;

  public ServiceDiscoveryInitializer(ConsulService consulService) {
    this.consulService = consulService;
  }

  @EventListener
  public void onApplicationEvent(AvailabilityChangeEvent<ReadinessState> event) {
    if (event.getState() == ReadinessState.ACCEPTING_TRAFFIC) {
      logger.debug("Received event to register service");
      consulService.register();
    } else if (event.getState() == ReadinessState.REFUSING_TRAFFIC) {
      logger.debug("Received event to DEregister service");
      consulService.deregister();
    }
  }
}
