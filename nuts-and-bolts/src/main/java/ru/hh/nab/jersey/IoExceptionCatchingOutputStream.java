package ru.hh.nab.jersey;

import java.io.IOException;
import java.io.OutputStream;

/*
*         Class contract
*
* <ol>
*   <li>
*         NEVER throw IOException or RuntimeException in any (overriden) method
*   </li>
*   <li>
*         Pass all operations to the delegate, as long as
*         they complete without throwing IOException or RuntimeException
*   </li>
*   <li>
*         When ANY method of delegate throws IOException
*         or RuntimeException, remember that exception, and
*         never call ANY methods of delegate after that.
*
*         <p>That includes calling close() method, so any
*         cleanup must be done by delegate creator.</p>
*   </li>
* </ol>
*/

final class IoExceptionCatchingOutputStream extends OutputStream {
  private final OutputStream delegate;
  private Exception exception;

  IoExceptionCatchingOutputStream(OutputStream delegate) {
    this.delegate = delegate;
  }

  IoExceptionCatchingOutputStream(IOException exception) {
    this.delegate = null;
    this.exception = exception;
  }

  // returns either IOException or RuntimeException thrown by last delegate call
  Exception getException() {
    return exception;
  }

  @Override
  public void write(int b) {
    if (exception == null) {
      try {
        delegate.write(b);
      } catch (IOException | RuntimeException ex) {
        exception = ex;
      }
    }
  }

  @Override
  public void write(byte[] b) {
    if (exception == null) {
      try {
        delegate.write(b);
      } catch (IOException  | RuntimeException ex) {
        exception = ex;
      }
    }
  }

  @Override
  public void write(byte[] b, int off, int len) {
    if (exception == null) {
      try {
        delegate.write(b, off, len);
      } catch (IOException  | RuntimeException ex) {
        exception = ex;
      }
    }
  }

  @Override
  public void flush() {
    if (exception == null) {
      try {
        delegate.flush();
      } catch (IOException  | RuntimeException ex) {
        exception = ex;
      }
    }
  }

  @Override
  public void close() {
    if (exception == null) {
      try {
        delegate.close();
      } catch (IOException  | RuntimeException ex) {
        exception = ex;
      }
    }
  }
}
