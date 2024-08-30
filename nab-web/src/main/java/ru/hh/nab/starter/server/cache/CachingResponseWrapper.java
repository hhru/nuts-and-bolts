package ru.hh.nab.starter.server.cache;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

class CachingResponseWrapper extends HttpServletResponseWrapper {
  private final ByteArrayOutputStream content = new ByteArrayOutputStream(1024);

  private ServletOutputStream outputStream;
  private PrintWriter writer;
  private boolean error = false;

  public CachingResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (outputStream == null) {
      try {
        outputStream = new ServletOutputStreamWrapper(super.getOutputStream());
      } catch (IOException e) {
        error = true;
        throw e;
      }
    }

    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (writer == null) {
      writer = new PrintWriter(getOutputStream());
    }

    return writer;
  }

  @Override
  public void resetBuffer() {
    super.resetBuffer();
    this.content.reset();
  }

  @Override
  public void reset() {
    super.reset();
    this.content.reset();
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    this.error = true;
    super.sendError(sc, msg);
  }

  @Override
  public void sendError(int sc) throws IOException {
    this.error = true;
    super.sendError(sc);
  }

  public boolean hasError() {
    return error;
  }

  public byte[] getContentAsByteArray() {
    return content.toByteArray();
  }

  private class ServletOutputStreamWrapper extends ServletOutputStream {
    private ServletOutputStream parent;

    public ServletOutputStreamWrapper(ServletOutputStream parent) {
      this.parent = parent;
    }

    @Override
    public void write(int b) throws IOException {
      try {
        parent.write(b);
        content.write(b);
      } catch (IOException e) {
        error = true;
        throw e;
      }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      try {
        parent.write(b, off, len);
        content.write(b, off, len);
      } catch (IOException e) {
        error = true;
        throw e;
      }
    }

    @Override
    public boolean isReady() {
      return parent.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      parent.setWriteListener(writeListener);
    }
  }
}
