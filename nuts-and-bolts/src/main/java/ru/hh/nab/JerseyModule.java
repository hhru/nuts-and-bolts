package ru.hh.nab;

import com.google.inject.AbstractModule;
import ru.hh.nab.jersey.FilteredXMLJAXBElementProvider;
import ru.hh.nab.jersey.FilteredXMLListElementProvider;
import ru.hh.nab.jersey.FilteredXMLRootElementProvider;
import ru.hh.nab.jersey.FreemarkerJerseyMarshaller;
import ru.hh.nab.jersey.JacksonJerseyMarshaller;

public class JerseyModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(StatsResource.class);
    bind(StatusResource.class);

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
