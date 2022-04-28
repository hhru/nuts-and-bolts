package ru.hh.nab.common.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ExceptionUtilsTest {

  @Test
  public void testGetOrThrowException() {
    var initialException = new Exception();
    var thrownException = throwWithGetOrThrow(initialException);

    assertSame(initialException, thrownException.getCause());
  }

  @Test
  public void testGetOrThrowInterruptedException() {
    var initialException = new InterruptedException();
    var thrownException = throwWithGetOrThrow(initialException);

    assertTrue(Thread.currentThread().isInterrupted());
    assertSame(initialException, thrownException.getCause());
  }

  @Test
  public void testGetOrThrowRuntimeException() {
    var initialException = new RuntimeException();
    var thrownException = throwWithGetOrThrow(initialException);

    assertSame(initialException, thrownException);
  }

  @Test
  public void testGetOrThrowCheckedMapping() {
    assertThrows(IllegalArgumentException.class, () -> ExceptionUtils.getOrThrow(() -> {
      throw new Exception();
    }, IllegalArgumentException::new));
  }

  private static RuntimeException throwWithGetOrThrow(Exception initialException) {
    return assertThrows(RuntimeException.class, () -> ExceptionUtils.getOrThrow(() -> {
      throw initialException;
    }));
  }
}
