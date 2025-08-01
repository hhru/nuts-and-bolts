package ru.hh.nab.web.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.errors.common.Errors;
import static ru.hh.nab.web.jersey.NabPriorities.LOW_PRIORITY;

@Priority(LOW_PRIORITY)
@APIResponse(
    responseCode = "500",
    description = "Internal Server Error",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class ExecutionExceptionMapper extends UnwrappingExceptionMapper<ExecutionException> {
}
