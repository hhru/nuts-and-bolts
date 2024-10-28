package ru.hh.nab.starter.server.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class CachingOutputStream extends OutputStream {
  private final ByteArrayOutputStream content = new ByteArrayOutputStream(1024);

  private final OutputStream delegate;

  public CachingOutputStream(OutputStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public void write(int b) throws IOException {
    delegate.write(b);
    content.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    delegate.write(b);
    content.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    delegate.write(b, off, len);
    content.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
    content.flush();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
    content.close();
  }

  public byte[] getContentAsByteArray() {
    return content.toByteArray();
  }
}
