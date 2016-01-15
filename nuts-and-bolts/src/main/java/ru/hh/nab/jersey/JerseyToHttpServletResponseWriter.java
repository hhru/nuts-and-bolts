package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

final class JerseyToHttpServletResponseWriter implements ContainerResponseWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(JerseyToHttpServletResponseWriter.class);

  private final HttpServletResponse response;
  private final boolean allowFlush;
  private IoExceptionCatchingOutputStream osWrapper;

  JerseyToHttpServletResponseWriter(HttpServletResponse response, boolean allowFlush) {
    this.response = response;
    this.allowFlush = allowFlush;
  }

  // Returns IOException if Grizzly encountered problems when sending response
  Exception getException() {
    return osWrapper == null ? null : osWrapper.getException();
  }

  @Override
  public OutputStream writeStatusAndHeaders(long contentLength,
                                            ContainerResponse cResponse) {
    response.setStatus(cResponse.getStatus());

    if (contentLength != -1 && contentLength < Integer.MAX_VALUE) {
      response.setContentLength((int) contentLength);
    }

    for (Map.Entry<String, List<Object>> e : cResponse.getHttpHeaders().entrySet()) {
      for (Object value : e.getValue()) {
        response.addHeader(e.getKey(), ContainerResponse.getHeaderValue(value));
      }
    }

    String contentType = response.getHeader("Content-Type");
    if (contentType != null) {
      response.setContentType(contentType);
    }

    try {
      if (allowFlush) {
        osWrapper =
          new IoExceptionCatchingOutputStream(response.getOutputStream());
      } else {
        osWrapper =
          new IoExceptionCatchingOutputStream(
            new NoFlushOutputStream(response.getOutputStream()));
      }
    } catch (IOException e) {
      return new IoExceptionCatchingOutputStream(e);
    }

    return osWrapper;
  }

  @Override
  public void finish() {
    if (!response.isCommitted()) {
      try {
        response.getOutputStream().flush();
      } catch (IOException exception) {
        // must have been already logged as error/warning elsewhere
        // with timeout, connection reset etc as the reason.
        LOGGER.debug("Exeption when flushing response", exception);
      }
    }
  }
}
