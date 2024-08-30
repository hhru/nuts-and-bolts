package ru.hh.nab.starter.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.jersey.message.internal.AcceptableMediaType;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_ACCEPT_ERRORS;

public class ErrorAcceptFilter implements ContainerResponseFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAcceptFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    if (responseContext.getStatus() >= 400) {
      String acceptErrors = requestContext.getHeaders().getFirst(X_HH_ACCEPT_ERRORS);
      if (acceptErrors != null) {
        try {
          List<AcceptableMediaType> acceptableMediaTypes = HttpHeaderReader.readAcceptMediaType(acceptErrors);
          if (!acceptableMediaTypes.isEmpty()) {
            responseContext.getHeaders().replace(CONTENT_TYPE, new ArrayList<>(List.of(acceptableMediaTypes.get(0).toString())));
          } else {
            LOGGER.warn("No valid AcceptableMediaType for errors found in {} header: {}", X_HH_ACCEPT_ERRORS, acceptErrors);
          }
        } catch (ParseException e) {
          LOGGER.warn("Error while parsing {} header.", X_HH_ACCEPT_ERRORS, e);
        }
      }
    }
  }
}
