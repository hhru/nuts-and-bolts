package ru.hh.nab.starter.exceptions;

import javax.annotation.Priority;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.errors.common.Errors;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
@APIResponse(
    responseCode = "404",
    description = "Not Found",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class NotFoundExceptionMapper extends NabExceptionMapper<NotFoundException> {
  public NotFoundExceptionMapper() {
    super(null, LoggingLevel.NOTHING);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, NotFoundException exception) {
    return exception.getResponse();
  }
}
