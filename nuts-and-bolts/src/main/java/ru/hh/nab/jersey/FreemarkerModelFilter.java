package ru.hh.nab.jersey;

import com.google.common.collect.Maps;
import com.google.inject.internal.cglib.proxy.Enhancer;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import java.lang.annotation.Annotation;
import java.util.Map;
import ru.hh.util.Proxies;

public class FreemarkerModelFilter implements ResourceFilter {
  private static final ContainerResponseFilter INSTANCE = new ContainerResponseFilter() {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
      Object entity = response.getEntity();
      if (entity == null)
        return response;

      Class<?> klass = Proxies.realClass(entity);
      if (klass == null)
        return response;

      FreemarkerTemplate ann = klass.getAnnotation(FreemarkerTemplate.class);
      if (ann != null) {
        Map<String,Object> reqMap = Maps.newHashMap(request.getProperties());
        String backurl = request.getRequestUri().toASCIIString();
        reqMap.put("backurl", backurl);
//        reqMap.put("backurl_encoded", backurl);
        response.setEntity(new FreemarkerModel(reqMap, entity, ann));
      }
      return response;
    }
  };

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return null;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return INSTANCE;
  }
}
