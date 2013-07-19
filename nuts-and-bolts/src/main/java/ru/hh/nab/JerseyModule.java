package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.RequestScoped;
import ru.hh.nab.jersey.FreemarkerJerseyMarshaller;
import ru.hh.nab.jersey.JacksonJerseyMarshaller;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.jersey.FilteredXMLJAXBElementProvider;
import ru.hh.nab.jersey.FilteredXMLListElementProvider;
import ru.hh.nab.jersey.FilteredXMLRootElementProvider;

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

    bind(FilteredXMLRootElementProvider.App.class);
    bind(FilteredXMLRootElementProvider.General.class);
    bind(FilteredXMLRootElementProvider.Text.class);

    bind(FilteredXMLListElementProvider.App.class);
    bind(FilteredXMLListElementProvider.General.class);
    bind(FilteredXMLListElementProvider.Text.class);

    bind(FilteredXMLJAXBElementProvider.App.class);
    bind(FilteredXMLJAXBElementProvider.General.class);
    bind(FilteredXMLJAXBElementProvider.Text.class);
  }
}
