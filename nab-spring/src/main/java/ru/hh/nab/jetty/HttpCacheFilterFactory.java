package ru.hh.nab.jetty;

import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.servlet.FilterHolder;
import ru.hh.filter.CacheFilter;
import ru.hh.nab.util.FileSettings;

import java.util.concurrent.ScheduledExecutorService;

public class HttpCacheFilterFactory {

  public static FilterHolder createCacheFilterHolder(FileSettings fileSettings,
                                                     String serviceName,
                                                     StatsDClient statsDClient,
                                                     ScheduledExecutorService scheduledExecutorService) {
    FilterHolder holder = new FilterHolder();
    String size = fileSettings.getString("http.cache.sizeInMB");
    if (size != null) {
      holder.setFilter(new CacheFilter(serviceName, Integer.parseInt(size), statsDClient, scheduledExecutorService));
    }
    return holder;
  }
}
