package ru.hh.nab.web;

import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.events.JettyBeforeStopEvent;
import ru.hh.nab.starter.events.JettyStartedEvent;

public class JettyEventListener {
  private static final Logger logger = LoggerFactory.getLogger(JettyEventListener.class);

  private final ConsulService consulService;

  public JettyEventListener(ConsulService consulService) {
    this.consulService = consulService;
  }

  @EventListener
  public void onApplicationEvent(JettyStartedEvent ignore) {
    logger.debug("Sending event to register service");
    ofNullable(consulService).ifPresent(ConsulService::register);
  }

  @EventListener
  public void onApplicationEvent(JettyBeforeStopEvent ignore) {
    logger.debug("Sending event to DEregister service");
    ofNullable(consulService).ifPresent(ConsulService::deregister);
  }
}
