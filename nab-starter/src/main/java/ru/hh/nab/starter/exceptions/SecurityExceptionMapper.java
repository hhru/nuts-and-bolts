package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.errors.common.Errors;
import static ru.hh.nab.starter.jersey.NabPriorities.LOW_PRIORITY;

@Priority(LOW_PRIORITY)
@APIResponse(
    responseCode = "403",
    description = "Forbidden",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class SecurityExceptionMapper extends NabExceptionMapper<SecurityException> {
  public SecurityExceptionMapper() {
    super(Response.Status.FORBIDDEN, LoggingLevel.INFO_WITHOUT_STACK_TRACE);
  }
}
