package ru.hh.nab.datasource;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompressedStackFactoryTest {

  @Test
  public void create() throws Exception {
    CompressedStackFactory compressedStackFactory = new CompressedStackFactory(
        InnerClass.class.getName(), "inner",
        OuterClass.class.getName(), "outer",
        new String[]{"ru.hh."}, new String[]{"ExcludeClass"});

    String compressedStack = OuterClass.outer(compressedStackFactory);

    assertEquals(
        "CompressedStackFactoryTest$OuterClass.inner>CompressedStackFactoryTest$MiddleClass.inner>CompressedStackFactoryTest$InnerClass.outer",
        compressedStack
    );
  }

  static class OuterClass {
    static String outer(CompressedStackFactory compressedStackFactory) {
      return inner(compressedStackFactory);
    }
    private static String inner(CompressedStackFactory compressedStackFactory) {
      return MiddleClass.outer(compressedStackFactory);
    }
  }

  static class MiddleClass {
    static String outer(CompressedStackFactory compressedStackFactory) {
      return inner(compressedStackFactory);
    }
    private static String inner(CompressedStackFactory compressedStackFactory) {
      return ExcludeClass.method(compressedStackFactory);
    }
  }

  static class ExcludeClass {
    static String method(CompressedStackFactory compressedStackFactory) {
      return InnerClass.outer(compressedStackFactory);
    }
  }

  static class InnerClass {
    static String outer(CompressedStackFactory compressedStackFactory) {
      return inner(compressedStackFactory);
    }
    static String inner(CompressedStackFactory compressedStackFactory) {
      return compressedStackFactory.create();
    }
  }
}
