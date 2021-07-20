package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.springframework.context.ApplicationEventPublisher;
import ru.hh.nab.starter.events.JettyBeforeStopEvent;
import ru.hh.nab.starter.events.JettyStartedEvent;

public class JettyLifeCycleListener extends AbstractLifeCycle.AbstractLifeCycleListener {
  private final ApplicationEventPublisher eventPublisher;

  public JettyLifeCycleListener(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void lifeCycleStarted(LifeCycle lifeCycle) {
    eventPublisher.publishEvent(new JettyStartedEvent());
  }

  @Override
  public void lifeCycleStopping(LifeCycle event) {
    eventPublisher.publishEvent(new JettyBeforeStopEvent());
  }
}
