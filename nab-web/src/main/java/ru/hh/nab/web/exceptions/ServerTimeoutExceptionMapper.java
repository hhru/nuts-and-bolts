package ru.hh.nab.starter.exceptions;

import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.deadline.context.ServerTimeoutException;
import ru.hh.errors.common.Errors;
import ru.hh.nab.starter.http.HttpStatus;

@APIResponse(
    responseCode = "577",
    description = "Server Timeout",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class ServerTimeoutExceptionMapper extends NabExceptionMapper<ServerTimeoutException> {
  public ServerTimeoutExceptionMapper() {
    super(HttpStatus.SERVER_TIMEOUT, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
