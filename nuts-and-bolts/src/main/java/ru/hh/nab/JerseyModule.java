package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.RequestScoped;
import ru.hh.nab.jersey.FreemarkerJerseyMarshaller;
import ru.hh.nab.jersey.JacksonJerseyMarshaller;
import ru.hh.nab.scopes.RequestScope;

public class JerseyModule extends AbstractModule {
  public final RequestScope REQUEST_SCOPE = new RequestScope();

  public JerseyModule() {
  }

  @Override
  protected void configure() {
    bind(StatsResource.class);
    bind(StatusResource.class);
    bindScope(RequestScoped.class, REQUEST_SCOPE);
    bind(FreemarkerJerseyMarshaller.class);
    bind(JacksonJerseyMarshaller.class);
  }
}
