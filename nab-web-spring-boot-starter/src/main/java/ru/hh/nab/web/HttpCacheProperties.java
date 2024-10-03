package ru.hh.nab.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(HttpCacheProperties.PREFIX)
public class HttpCacheProperties {

  public static final String PREFIX = "http.cache";
  public static final String HTTP_CACHE_SIZE_PROPERTY = "http.cache.sizeInMb";

  private final DataSize size;

  public HttpCacheProperties(@DataSizeUnit(DataUnit.MEGABYTES) DataSize sizeInMb) {
    this.size = sizeInMb;
  }

  public DataSize getSize() {
    return size;
  }
}
