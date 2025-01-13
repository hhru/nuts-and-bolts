package ru.hh.nab.starter.server.logging;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;
import ru.hh.nab.starter.jersey.NabPriorities;

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
