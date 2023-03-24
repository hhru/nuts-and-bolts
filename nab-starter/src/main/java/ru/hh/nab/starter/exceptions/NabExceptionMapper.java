package ru.hh.nab.starter.exceptions;

import static java.util.Optional.ofNullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * This exception mapper solves several tasks:
 * — map exceptions to specific HTTP response codes and log them with appropriate levels
 * — provide a mechanism to add generic exception serializers (see {@link ExceptionSerializer})
 * {@link ExceptionSerializer} beans must be present in application context.
 */
public abstract class NabExceptionMapper<T extends Exception> implements ExceptionMapper<T> {
  public static final int LOW_PRIORITY = Priorities.USER + 1;

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
    return applicationContext.getBeansOfType(ExceptionSerializer.class).values().stream()
      .filter(s -> s.isCompatible(request, response))
      .findFirst()
      .map(s -> s.serializeException(statusCode, exception))
      .orElseGet(() -> Response.status(statusCode).entity(ofNullable(exception.getMessage()).orElse("")).type(TEXT_PLAIN).build());
  }
}
