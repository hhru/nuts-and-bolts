package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import org.glassfish.grizzly.http.server.Response;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

final class JerseyToGrizzlyResponseWriter implements ContainerResponseWriter {
  private final Response response;
  private final boolean allowFlush;
  private IoExceptionCatchingOutputStream osWrapper;

  JerseyToGrizzlyResponseWriter(Response response, boolean allowFlush) {
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

    if (allowFlush) {
      osWrapper =
        new IoExceptionCatchingOutputStream(response.getOutputStream());
    } else {
      osWrapper =
        new IoExceptionCatchingOutputStream(
          new NoFlushOutputStream(response.getOutputStream()));
    }

    return osWrapper;
  }

  @Override
  public void finish() {
    if (!response.isCommitted()) {
      response.finish();
    }
  }
}
