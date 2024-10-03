package ru.hh.nab.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(JaxbProperties.PREFIX)
public class JaxbProperties {

  public static final String PREFIX = "jaxb";

  private int contextsMaxCollectionSize = 256;

  public int getContextsMaxCollectionSize() {
    return contextsMaxCollectionSize;
  }

  public void setContextsMaxCollectionSize(int contextsMaxCollectionSize) {
    this.contextsMaxCollectionSize = contextsMaxCollectionSize;
  }
}
