package ru.hh.nab.security;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;

public class SecurityFilter implements ResourceFilter {
  static final String REQUEST_PROPERTY_KEY = "ru.hh.nab.security.SecurityFilter.permissions";
  private final PermissionLoader permissions;

  private final SecurityRequestFilter filterInstance = new SecurityRequestFilter();

  @Inject
  public SecurityFilter(PermissionLoader permissions) {
    this.permissions = permissions;
  }

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return filterInstance;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return null;
  }

  public class SecurityRequestFilter implements ContainerRequestFilter {
    @Override
    public ContainerRequest filter(ContainerRequest request) {
      String apiKey = request.getHeaderValue("X-Hh-Api-Key");

      Permissions p = null;
      if (!StringUtils.isEmpty(apiKey)) {
        p = permissions.forKey(apiKey);
      }
      if (p == null) {
        p = permissions.anonymous();
      }

      request.getProperties().put(REQUEST_PROPERTY_KEY, p);
      return request;
    }
  }
}
