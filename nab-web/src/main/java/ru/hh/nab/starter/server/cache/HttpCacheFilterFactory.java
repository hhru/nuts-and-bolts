package ru.hh.nab.starter.server.cache;

import org.eclipse.jetty.servlet.FilterHolder;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;

public class HttpCacheFilterFactory {
  private HttpCacheFilterFactory() {}

  public static FilterHolder createCacheFilterHolder(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) {
    FilterHolder holder = new FilterHolder();
    String size = fileSettings.getString("http.cache.sizeInMB");
    if (size != null) {
      holder.setFilter(new CacheFilter(serviceName, Integer.parseInt(size), statsDSender));
    }
    return holder;
  }
}
