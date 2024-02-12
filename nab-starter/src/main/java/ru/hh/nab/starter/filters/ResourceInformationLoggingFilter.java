package ru.hh.nab.starter.filters;

import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Optional;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;
import org.springframework.util.ClassUtils;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CODE_FUNCTION_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.CODE_NAMESPACE_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.HTTP_ROUTE_MDC_KEY;

public class ResourceInformationLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final String SLASH = "/";
  @Inject
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Class<?> controllerClass = ClassUtils.getUserClass(resourceInfo.getResourceClass());
    String resourceMethodName = resourceInfo.getResourceMethod().getName();
    String controller = controllerClass.getSimpleName() + '#' + resourceMethodName;

    requestContext.setProperty(CONTROLLER_MDC_KEY, controller);
    requestContext.setProperty(CODE_FUNCTION_MDC_KEY, resourceMethodName);
    requestContext.setProperty(CODE_NAMESPACE_MDC_KEY, controllerClass.getCanonicalName());
    getHttpRoute(requestContext.getUriInfo()).ifPresent(route -> requestContext.setProperty(HTTP_ROUTE_MDC_KEY, route));

    MDC.setController(controller);
  }

  private Optional<String> getHttpRoute(UriInfo uriInfo) {
    return ofNullable(uriInfo)
        .filter(ExtendedUriInfo.class::isInstance)
        .map(ExtendedUriInfo.class::cast)
        .map(ExtendedUriInfo::getMatchedTemplates)
        .map(
            templates -> {
              //The base path where the resource is registered or /
              String basePath = of(uriInfo)
                  .map(UriInfo::getBaseUri)
                  .map(URI::getPath)
                  .orElse(SLASH);
              UriBuilder uriBuilder = UriBuilder.fromPath(basePath);
              //Entries in matchedTemplates are ordered in reverse order with the root template last
              Lists.reverse(templates).stream().map(UriTemplate::getTemplate).forEach(uriBuilder::path);
              return uriBuilder.toTemplate();
            }
        );
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.clearController();
  }
}
