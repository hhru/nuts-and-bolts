package ru.hh.nab.web.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import ru.hh.errors.common.Errors;
import ru.hh.jclient.common.HttpStatuses;
import static ru.hh.jclient.common.HttpStatuses.BAD_GATEWAY;
import static ru.hh.jclient.common.HttpStatuses.INTERNAL_SERVER_ERROR;
import ru.hh.nab.starter.http.HttpStatus;
import static ru.hh.nab.web.jersey.NabPriorities.LOW_PRIORITY;

@Priority(LOW_PRIORITY)
@APIResponses(
    {
        @APIResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    implementation = Errors.class
                )
            )
        ),
        @APIResponse(
            responseCode = "502",
            description = "Bad Gateway",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(
                    implementation = Errors.class
                )
            )
        )
    }
)
public class WebApplicationExceptionMapper extends NabExceptionMapper<WebApplicationException> {

  private static final List<Integer> ALWAYS_LOGGABLE_ERRORS = List.of(INTERNAL_SERVER_ERROR, BAD_GATEWAY);

  public WebApplicationExceptionMapper() {
    super(null, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  protected void logException(WebApplicationException wae, LoggingLevel loggingLevel) {
    if (wae.getCause() == null && !ALWAYS_LOGGABLE_ERRORS.contains(wae.getResponse().getStatus())) {
      return;
    }
    if (wae.getResponse().getStatus() < 500) {
      loggingLevel = LoggingLevel.INFO_WITH_STACK_TRACE;
    }
    if (wae.getResponse().getStatus() == BAD_GATEWAY) {
      loggingLevel = LoggingLevel.DEBUG_WITH_STACK_TRACE;
    }
    super.logException(wae, loggingLevel);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, WebApplicationException exception) {
    return switch (exception.getResponse().getStatus()) {
      case HttpStatuses.SERVER_TIMEOUT -> Response.status(Response.Status.GATEWAY_TIMEOUT).build();
      case HttpStatuses.INSUFFICIENT_TIMEOUT -> Response.status(HttpStatus.SERVER_TIMEOUT).build();
      default -> exception.getResponse();
    };
  }
}
