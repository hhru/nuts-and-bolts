package ru.hh.nab.common.servlet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class UriComponentTest {
  @Test
  public void testDecode() {
    var fullKey = "fullForm";
    var values = List.of("value1", "value2");
    var shortKey = "shortForm";
    String queryString = Stream.concat(Stream.of(shortKey), values.stream().map(value -> String.join("=", fullKey, value)))
      .collect(Collectors.joining("&"));
    Map<String, List<String>> result = UriComponent.decodeQuery(queryString, true, true);
    List<String> actual = result.get(fullKey);
    assertEquals(values, actual);
    List<String> shortParam = result.get(shortKey);
    assertEquals(1, shortParam.size());
    assertEquals("", shortParam.get(0));
  }
}
