package ru.hh.nab.starter.exceptions;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import ru.hh.errors.common.Errors;
import ru.hh.nab.starter.jersey.NabPriorities;

/**
 * This exception mapper solves several tasks:
 * — map exceptions to specific HTTP response codes and log them with appropriate levels
 * — provide a mechanism to add generic exception serializers (see {@link ExceptionSerializer})
 * {@link ExceptionSerializer} beans must be present in application context.
 */
public abstract class NabExceptionMapper<T extends Exception> implements ExceptionMapper<T> {

  /**
   * @deprecated Use {@link NabPriorities#LOW_PRIORITY}
   */
  @Deprecated(forRemoval = true)
  public static final int LOW_PRIORITY = NabPriorities.LOW_PRIORITY;

  protected static final Logger LOGGER = LoggerFactory.getLogger(NabExceptionMapper.class);

  protected final Response.StatusType defaultStatus;
  protected final LoggingLevel defaultLoggingLevel;

  @Context
  protected HttpServletRequest request;
  @Context
  protected HttpServletResponse response;
  @Inject
  protected ApplicationContext applicationContext;

  protected enum LoggingLevel {
    NOTHING,
    ERROR_WITH_STACK_TRACE,
    WARN_WITH_STACK_TRACE,
    WARN_WITHOUT_STACK_TRACE,
    INFO_WITH_STACK_TRACE,
    INFO_WITHOUT_STACK_TRACE,
    DEBUG_WITH_STACK_TRACE,
  }

  public NabExceptionMapper(Response.StatusType defaultStatus, LoggingLevel defaultLoggingLevel) {
    this.defaultStatus = defaultStatus;
    this.defaultLoggingLevel = defaultLoggingLevel;
  }

  @Override
  public Response toResponse(T exception) {
    return toResponseInternal(defaultStatus, defaultLoggingLevel, exception);
  }

  protected Response toResponseInternal(Response.StatusType status, LoggingLevel loggingLevel, T exception) {
    logException(exception, loggingLevel);
    return serializeException(status, exception);
  }

  void logException(T exception, LoggingLevel loggingLevel) {
    switch (loggingLevel) {
      case NOTHING: {
        break;
      }
      case ERROR_WITH_STACK_TRACE: {
        LOGGER.error(exception.getMessage(), exception);
        break;
      }
      case WARN_WITH_STACK_TRACE: {
        LOGGER.warn(exception.getMessage(), exception);
        break;
      }
      case WARN_WITHOUT_STACK_TRACE: {
        LOGGER.warn(exception.getMessage());
        break;
      }
      case INFO_WITH_STACK_TRACE: {
        LOGGER.info(exception.getMessage(), exception);
        break;
      }
      case INFO_WITHOUT_STACK_TRACE: {
        LOGGER.info(exception.getMessage());
        break;
      }
      case DEBUG_WITH_STACK_TRACE: {
        LOGGER.debug(exception.getMessage(), exception);
        break;
      }
      default: {
        LOGGER.error("Exception with unsupported logging type: {}", loggingLevel, exception);
      }
    }
  }

  protected Response serializeException(Response.StatusType statusCode, T exception) {
    return applicationContext
        .getBeansOfType(ExceptionSerializer.class)
        .values()
        .stream()
        .filter(s -> s.isCompatible(request, response))
        .findFirst()
        .map(s -> s.serializeException(statusCode, exception))
        .orElseGet(() -> {
          Errors errors = new Errors(
              statusCode.getStatusCode(),
              exception.getClass().getCanonicalName(),
              ofNullable(exception.getMessage()).orElse("")
          );
          return Response.status(statusCode).type(APPLICATION_JSON).entity(errors).build();
        });
  }
}
