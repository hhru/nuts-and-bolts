package ru.hh.nab.jersey;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public final class FreemarkerModel {
  public final Map<String, Object> requestProperties;
  public final Object appModel;
  public final FreemarkerTemplate annotation;

  FreemarkerModel(Map<String, Object> requestProperties, Object appModel) {
    this.requestProperties = requestProperties;
    this.appModel = appModel;
    this.annotation = appModel.getClass().getAnnotation(FreemarkerTemplate.class);
  }

  public static FreemarkerModel of(Object o) {
    return new FreemarkerModel(ImmutableMap.<String, Object>of(), o);
  }
}
