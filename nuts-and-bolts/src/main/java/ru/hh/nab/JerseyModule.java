package ru.hh.nab;

import com.google.inject.AbstractModule;

public class JerseyModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(StatsResource.class);
    bind(StatusResource.class);
  }
}
