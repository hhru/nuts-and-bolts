package ru.hh.nab.jersey;

import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

final class JerseyToHttpServletResponseWriter implements ContainerResponseWriter {

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private IoExceptionCatchingOutputStream osWrapper;

  private static final OutputStream NULL_OS = new OutputStream() {
    @Override
    public void write(int b) throws IOException {
    }
  };

  JerseyToHttpServletResponseWriter(HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
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
      final String headerName = e.getKey();
      for (Object oValue : e.getValue()) {
        final String headerValue = ContainerResponse.getHeaderValue(oValue);
        response.addHeader(headerName, headerValue);
      }
    }

    try {
      osWrapper =
        new IoExceptionCatchingOutputStream(response.getOutputStream());
    } catch (IOException e) {
      return new IoExceptionCatchingOutputStream(e);
    }

    return osWrapper;
  }

  @Override
  public void finish() {
    if (osWrapper != null && !response.isCommitted()) {
      osWrapper.close();
    }
  }
}
