package ru.hh.nab.jersey;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import ru.hh.util.Proxies;

public final class FreemarkerModel {
  public final Map<String, Object> requestProperties;
  public final Object appModel;
  public final FreemarkerTemplate annotation;

  FreemarkerModel(Map<String, Object> requestProperties, Object appModel,
                  FreemarkerTemplate annotation) {
    this.requestProperties = requestProperties;
    this.appModel = appModel;
    this.annotation = annotation;
  }

  @SuppressWarnings({"unchecked"})
  public static FreemarkerModel of(Object o) {
    FreemarkerTemplate ann = Proxies.realClass(o).
            getAnnotation(FreemarkerTemplate.class);
    return new FreemarkerModel(ImmutableMap.<String, Object>of(), o, ann);
  }
}
