package ru.hh.nab.datasource.monitoring;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ru.hh.nab.datasource.monitoring.stack.CompressedStackFactory;
import ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryConfig;

public class CompressedStackFactoryTest {

  @Test
  public void create() {
    CompressedStackFactoryConfig config = new CompressedStackFactoryConfig.Builder()
        .withInnerClassExcluding(InnerClass.class.getName())
        .withInnerMethodExcluding("inner")
        .withOuterClassExcluding(OuterClass.class.getName())
        .withOuterMethodExcluding("outer")
        .withIncludePackages(new String[]{"ru.hh."})
        .withExcludeClassesParts(new String[]{"ExcludeClass"})
        .build();

    CompressedStackFactory compressedStackFactory = new CompressedStackFactory(config);

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
