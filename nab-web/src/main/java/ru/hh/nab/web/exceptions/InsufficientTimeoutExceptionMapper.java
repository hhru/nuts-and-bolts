package ru.hh.nab.starter.exceptions;

import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.deadline.context.InsufficientTimeoutException;
import ru.hh.errors.common.Errors;
import static ru.hh.nab.starter.http.HttpStatus.INSUFFICIENT_TIMEOUT;

@APIResponse(
    responseCode = "477",
    description = "Insufficient timeout",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class InsufficientTimeoutExceptionMapper extends NabExceptionMapper<InsufficientTimeoutException> {
  public InsufficientTimeoutExceptionMapper() {
    super(INSUFFICIENT_TIMEOUT, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
