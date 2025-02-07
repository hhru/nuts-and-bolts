package ru.hh.nab.web.jersey.interceptor;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;
import ru.hh.nab.web.jersey.NabPriorities;

@Priority(NabPriorities.OBSERVABILITY)
public class SentryAppenderInterceptor implements WriterInterceptor {

  @Context
  protected HttpServletRequest request;

  @Override
  public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
    if (MDC.getController().isEmpty()) {
      Optional.ofNullable(request.getAttribute(CONTROLLER_MDC_KEY)).ifPresent(controller -> MDC.setController(Objects.toString(controller)));
      context.proceed();
      MDC.clearController();
    } else {
      context.proceed();
    }
  }
}
