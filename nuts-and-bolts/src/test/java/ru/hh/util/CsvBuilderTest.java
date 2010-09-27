package ru.hh.util;

import org.junit.Assert;
import org.junit.Test;

public class CsvBuilderTest {
  @Test
  public void testDQuotes() {
    Assert.assertEquals("\"\"\"\"", new CsvBuilder().put("\"").build());
    Assert.assertEquals("aaa,\"b\"\"bb\",ccc", new CsvBuilder().put("aaa", "b\"bb", "ccc").build());
    Assert.assertEquals("\"a\rb\n\",b", new CsvBuilder().put("a\rb\n", "b").build());
    Assert.assertEquals("\"a,b\",b", new CsvBuilder().put("a,b", "b").build());
  }
}
