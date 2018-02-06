package ru.hh.nab.hibernate.datasource;

import java.lang.annotation.Annotation;

public enum DataSourceType {

  DEFAULT(Default.class, "default-db"),
  REPLICA(Replica.class, "replica-db");

  private final Class<? extends Annotation> annotation;
  private final String id;

  DataSourceType(Class<? extends Annotation> annotation, String id) {
    this.annotation = annotation;
    this.id = id;
  }

  public Class<? extends Annotation> getAnnotation() {
    return annotation;
  }

  public String getId() {
    return id;
  }
}
