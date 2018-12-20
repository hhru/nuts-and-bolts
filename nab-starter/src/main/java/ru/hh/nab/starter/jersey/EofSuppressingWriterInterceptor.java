package ru.hh.nab.starter.jersey;

import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

@Provider
public class EofSuppressingWriterInterceptor implements WriterInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(EofSuppressingWriterInterceptor.class);

  @Override
  public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
    try {
      context.proceed();
    } catch (EofException e) {
      LOGGER.warn("{}({}) occured: client is not listening anymore", e.getClass().getName(), e.getMessage());
    }
  }
}
