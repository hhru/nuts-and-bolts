package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import ru.hh.filter.CacheFilter;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.jetty.JettyServerFactory;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

class JettyServerModule extends AbstractModule {

  @Override
  protected void configure() { }

  @Provides
  @Singleton
  protected Server jettyServer(Settings settings, JerseyHttpServlet jerseyHttpServlet, @Named("cacheFilter") FilterHolder cacheFilter) {
    return JettyServerFactory.create(settings, jerseyHttpServlet, cacheFilter);
  }

  @Provides
  @Named("cacheFilter")
  @Singleton
  private FilterHolder cacheFilter(@Named("serviceName") String serviceName, @Named("settings.properties") Properties properties,
                                   StatsDClient statsDClient, ScheduledExecutorService scheduledExecutorService) {
    FilterHolder holder = new FilterHolder();

    String size = properties.getProperty("http.cache.sizeInMB");
    if (size != null) {
      holder.setFilter(new CacheFilter(serviceName, Integer.parseInt(size), statsDClient, scheduledExecutorService));
    }

    return holder;
  }
}
