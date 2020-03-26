package ru.hh.nab.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

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
}
