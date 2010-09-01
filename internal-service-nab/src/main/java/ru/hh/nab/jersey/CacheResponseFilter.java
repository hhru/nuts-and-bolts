package ru.hh.nab.jersey;

import com.google.common.base.Preconditions;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.util.Date;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang.ArrayUtils;

public class CacheResponseFilter implements ContainerResponseFilter {
  private static CacheControl CC_NO_CACHE = makeCacheControlNoCache();
  private Cached annotation;

  public CacheResponseFilter(Cached annotation) {
    Preconditions.checkNotNull(annotation);
    this.annotation = annotation;
  }

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    int maxAge = annotation.value();
    if (annotation.statusCodes().length != 0) {
      if (!ArrayUtils.contains(annotation.statusCodes(), response.getStatus()))
        maxAge = 0;
    }
    MultivaluedMap<String, Object> headers = response.getHttpHeaders();
    if (maxAge > 0) {
      headers.putSingle("Cache-Control", makeCacheControl(maxAge));
      headers.putSingle("Expires", new Date(System.currentTimeMillis() + maxAge));
    } else {
      headers.putSingle("Cache-Control", CC_NO_CACHE);
      headers.add("Pragma", "no-cache");
      headers.putSingle("Expires", new Date(0));
    }
    return response;
  }

  private static CacheControl makeCacheControlNoCache() {
    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    return cc;
  }

  private static CacheControl makeCacheControl(int age) {
    CacheControl cc = new CacheControl();
    cc.setMaxAge(age);
    cc.setSMaxAge(age);
    return cc;
  }
}
