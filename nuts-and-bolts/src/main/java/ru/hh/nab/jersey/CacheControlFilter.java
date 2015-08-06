package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import java.util.Date;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.ArrayUtils;

class CacheControlFilter implements ResourceFilter {
  private final Cached ann;

  public CacheControlFilter(Cached ann) {
    this.ann = ann;
  }

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return null;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return new ContainerResponseFilter() {
      @Override
      public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        int maxAge = ann.value();
        if (ann.statusCodes().length != 0) {
          if (!ArrayUtils.contains(ann.statusCodes(), response.getStatus())) {
            maxAge = 0;
          }
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
    };
  }

  private static CacheControl CC_NO_CACHE = makeCacheControlNoCache();

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
