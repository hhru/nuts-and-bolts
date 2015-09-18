package ru.hh.nab.jersey;

import java.io.IOException;
import java.io.OutputStream;

/*
* Class contract : pass all operations to the delegate,
* except for flush() calls. Calls to flush() are NOOP.
*/

final class NoFlushOutputStream extends OutputStream {
  private final OutputStream delegate;

  NoFlushOutputStream(OutputStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public void write(int b) throws IOException {
    delegate.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    delegate.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    delegate.write(b, off, len);
  }

  @Override
  public void flush() {
    // no flush
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
