package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.errors.common.Errors;
import static ru.hh.nab.starter.jersey.NabPriorities.LOW_PRIORITY;

@Priority(LOW_PRIORITY)
@APIResponse(
    responseCode = "409",
    description = "Conflict",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class IllegalStateExceptionMapper extends NabExceptionMapper<IllegalStateException> {
  public IllegalStateExceptionMapper() {
    super(CONFLICT, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
