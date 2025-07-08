package ru.hh.nab.starter.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.nab.starter.http.HttpStatus;

public class WebApplicationExceptionMapperTest {

  private WebApplicationExceptionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new WebApplicationExceptionMapper();
  }

  @Test
  void testToResponseInternalWithServerTimeout() {
    // Given
    WebApplicationException exception = createWebApplicationException(HttpStatuses.SERVER_TIMEOUT);
    Response.StatusType status = INTERNAL_SERVER_ERROR;
    NabExceptionMapper.LoggingLevel loggingLevel = NabExceptionMapper.LoggingLevel.ERROR_WITH_STACK_TRACE;

    // When
    Response response = mapper.toResponseInternal(status, loggingLevel, exception);

    // Then
    assertEquals(GATEWAY_TIMEOUT.getStatusCode(), response.getStatus());
  }

  @Test
  void testToResponseInternalWithInsufficientTimeout() {
    // Given
    WebApplicationException exception = createWebApplicationException(HttpStatuses.INSUFFICIENT_TIMEOUT);
    Response.StatusType status = INTERNAL_SERVER_ERROR;
    NabExceptionMapper.LoggingLevel loggingLevel = NabExceptionMapper.LoggingLevel.ERROR_WITH_STACK_TRACE;

    // When
    Response response = mapper.toResponseInternal(status, loggingLevel, exception);

    // Then
    assertEquals(HttpStatus.SERVER_TIMEOUT.getStatusCode(), response.getStatus());
  }

  @Test
  void testToResponseInternalWithDefaultCase() {
    // Given
    int statusCode = HttpStatuses.BAD_GATEWAY;
    WebApplicationException exception = createWebApplicationException(statusCode);
    // When
    Response response = mapper.toResponse(exception);

    // Then
    assertEquals(statusCode, response.getStatus());
  }

  @Test
  void testToResponseInternalWithNullStatus() {
    // Given
    WebApplicationException exception = createWebApplicationException(500);
    Response.StatusType status = null;
    NabExceptionMapper.LoggingLevel loggingLevel = NabExceptionMapper.LoggingLevel.ERROR_WITH_STACK_TRACE;

    // When
    Response response = mapper.toResponseInternal(status, loggingLevel, exception);

    // Then
    assertEquals(500, response.getStatus());
  }

  private WebApplicationException createWebApplicationException(int statusCode) {
    return new WebApplicationException(Response.status(statusCode).build());
  }
}
