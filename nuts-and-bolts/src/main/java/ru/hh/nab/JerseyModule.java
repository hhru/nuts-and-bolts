package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import ru.hh.nab.jersey.FilteredXMLJAXBElementProvider;
import ru.hh.nab.jersey.FilteredXMLListElementProvider;
import ru.hh.nab.jersey.FilteredXMLRootElementProvider;
import ru.hh.nab.jersey.FreemarkerJerseyMarshaller;
import ru.hh.nab.jersey.JacksonJerseyMarshaller;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.jersey.JerseyResourceFilterFactory;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.Permissions;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

public class JerseyModule extends AbstractModule {
  private final WebApplication webapp;

  public JerseyModule(WebApplication webapp) {
    this.webapp = webapp;
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  protected JerseyHttpServlet jerseyHttpServlet(WebApplication wa) {
    return new JerseyHttpServlet(wa);
  }

  @Provides
  @Singleton
  protected ResourceConfig resourceConfig(Settings settings) {
    ResourceConfig ret = new DefaultResourceConfig(
      FreemarkerJerseyMarshaller.class,
      JacksonJerseyMarshaller.class,
      FilteredXMLRootElementProvider.App.class,
      FilteredXMLRootElementProvider.General.class,
      FilteredXMLRootElementProvider.Text.class,
      FilteredXMLListElementProvider.App.class,
      FilteredXMLListElementProvider.General.class,
      FilteredXMLListElementProvider.Text.class,
      FilteredXMLJAXBElementProvider.App.class,
      FilteredXMLJAXBElementProvider.General.class,
      FilteredXMLJAXBElementProvider.Text.class,
      StatsResource.class,
      StatusResource.class);

    ret.getProperties().put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, JerseyResourceFilterFactory.class.getName());

    boolean disableWadl = Boolean.parseBoolean(settings.subTree("jersey").getProperty("disableWadl", "true"));
    ret.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, disableWadl);

    return ret;
  }

  @Provides
  @Singleton
  protected GuiceComponentProviderFactory componentProviderFactory(ResourceConfig resources, Injector inj) {
    return new GuiceComponentProviderFactory(resources, inj);
  }

  private WebApplication getInitiatedWebapp(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    synchronized (webapp) {
      if (!webapp.isInitiated()) {
        webapp.initiate(resources, ioc);
      }
    }
    return webapp;
  }

  @Provides
  @Singleton
  protected WebApplication initializedWebApplication(ResourceConfig resources, GuiceComponentProviderFactory ioc) {
    return getInitiatedWebapp(resources, ioc);
  }

  @Provides
  @RequestScoped
  protected Permissions permissions(HttpServletRequest req, PermissionLoader permissions) {
    String apiKey = req.getHeader("X-Hh-Api-Key");
    Permissions ret = permissions.forKey(apiKey);
    if (ret != null) {
      return permissions.forKey(apiKey);
    }
    return permissions.anonymous();
  }
}
