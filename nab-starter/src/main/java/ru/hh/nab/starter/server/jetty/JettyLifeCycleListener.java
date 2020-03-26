package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.util.component.LifeCycle;
import org.springframework.context.ApplicationEventPublisher;
import ru.hh.nab.starter.JettyStartedEvent;

public class JettyLifeCycleListener implements LifeCycle.Listener {
  private final ApplicationEventPublisher eventPublisher;

  public JettyLifeCycleListener(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void lifeCycleStarting(LifeCycle lifeCycle) {
  }

  @Override
  public void lifeCycleStarted(LifeCycle lifeCycle) {
      eventPublisher.publishEvent(new JettyStartedEvent());
  }

  @Override
  public void lifeCycleFailure(LifeCycle lifeCycle, Throwable throwable) {
  }

  @Override
  public void lifeCycleStopping(LifeCycle lifeCycle) {
  }

  @Override
  public void lifeCycleStopped(LifeCycle lifeCycle) {
  }
}
