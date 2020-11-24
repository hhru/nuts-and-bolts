package ru.hh.nab.starter.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import ru.hh.nab.starter.ConsulService;

public class JettyEventListener {
  private static final Logger logger = LoggerFactory.getLogger(JettyEventListener.class);

  private final ConsulService consulService;

  public JettyEventListener(ConsulService consulService) {
    this.consulService = consulService;
  }

  @EventListener
  public void onApplicationEvent(JettyStartedEvent ignore) {
    logger.debug("Registering service in consul");
    consulService.register();
  }

  @EventListener
  public void onApplicationEvent(JettyBeforeStopEvent ignore) {
    logger.debug("Deregistering service in consul");
    consulService.deregister();
  }
}
