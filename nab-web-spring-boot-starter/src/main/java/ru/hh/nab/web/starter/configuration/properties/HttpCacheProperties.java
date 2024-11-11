package ru.hh.nab.web.starter.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(HttpCacheProperties.PREFIX)
public class HttpCacheProperties {

  public static final String PREFIX = "http.cache";
  public static final String HTTP_CACHE_SIZE_PROPERTY = "http.cache.sizeInMb";

  private int sizeInMb;

  public int getSizeInMb() {
    return sizeInMb;
  }

  public void setSizeInMb(int sizeInMb) {
    this.sizeInMb = sizeInMb;
  }
}
