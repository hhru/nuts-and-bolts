package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

final class JerseyToHttpServletResponseWriter implements ContainerResponseWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(JerseyToHttpServletResponseWriter.class);

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final boolean allowFlush;
  private IoExceptionCatchingOutputStream osWrapper;

  private static final OutputStream NULL_OS = new OutputStream() {
    @Override
    public void write(int b) throws IOException {
    }
  };

  JerseyToHttpServletResponseWriter(HttpServletRequest request, HttpServletResponse response, boolean allowFlush) {
    this.request = request;
    this.response = response;
    this.allowFlush = allowFlush;
  }

  // Returns IOException if there were problems when writing response to HttpServletResponse
  Exception getException() {
    return osWrapper == null ? null : osWrapper.getException();
  }

  @Override
  public OutputStream writeStatusAndHeaders(long contentLength,
                                            ContainerResponse cResponse) {
    if (response.isCommitted() || request.isAsyncStarted()) {
      return NULL_OS;
    }

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
    if (!response.isCommitted() && !request.isAsyncStarted()) {
      try {
        response.getOutputStream().flush();
      } catch (IOException exception) {
        // The exception must have been already logged as error/warning
        // elsewhere, so log it as debug only.
        LOGGER.debug("Exeption when flushing response", exception);
      }
    }
  }
}
