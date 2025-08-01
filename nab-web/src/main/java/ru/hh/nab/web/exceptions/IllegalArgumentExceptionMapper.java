package ru.hh.nab.web.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.errors.common.Errors;
import static ru.hh.nab.web.jersey.NabPriorities.LOW_PRIORITY;

@Priority(LOW_PRIORITY)
@APIResponse(
    responseCode = "400",
    description = "Bad Request",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class IllegalArgumentExceptionMapper extends NabExceptionMapper<IllegalArgumentException> {
  public IllegalArgumentExceptionMapper() {
    super(BAD_REQUEST, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
