package ru.hh.nab.neo.starter.server;

import org.eclipse.jetty.util.component.LifeCycle;
import org.springframework.context.ApplicationEventPublisher;
import ru.hh.nab.neo.starter.events.JettyBeforeStopEvent;
import ru.hh.nab.neo.starter.events.JettyStartedEvent;

public class JettyLifeCycleListener implements LifeCycle.Listener {
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
