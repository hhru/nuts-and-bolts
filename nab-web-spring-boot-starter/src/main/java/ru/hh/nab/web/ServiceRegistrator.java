package ru.hh.nab.web;

import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import ru.hh.nab.starter.consul.ConsulService;

public class ServiceRegistrator {
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistrator.class);

  private final ConsulService consulService;

  public ServiceRegistrator(ConsulService consulService) {
    this.consulService = consulService;
  }

  @EventListener
  public void onApplicationEvent(AvailabilityChangeEvent<ReadinessState> event) {
    if (event.getState() == ReadinessState.ACCEPTING_TRAFFIC) {
      logger.debug("Received event to register service");
      ofNullable(consulService).ifPresent(ConsulService::register);
    } else if (event.getState() == ReadinessState.REFUSING_TRAFFIC) {
      logger.debug("Received event to DEregister service");
      ofNullable(consulService).ifPresent(ConsulService::deregister);
    }
  }
}
