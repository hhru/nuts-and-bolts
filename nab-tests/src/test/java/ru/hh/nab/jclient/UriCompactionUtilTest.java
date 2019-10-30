package ru.hh.nab.jclient;

import java.util.StringJoiner;
import org.junit.Test;
import ru.hh.jclient.common.Uri;
import static org.junit.Assert.assertEquals;

public class UriCompactionUtilTest {

  @Test
  public void testCompactionWorksForNumbersAndHexHashes() {
    var replacement = "<>";
    Uri uri = Uri.create("http://localhost:2800/resource/123456/daba9e610001f70104003acc866d55656d6a5a/get");
    assertEquals(
      new StringJoiner("/", "/", "").add("resource").add(replacement).add(replacement).add("get").toString(),
      UriCompactionUtil.compactUri(uri, 4, 16, replacement)
    );
  }

  @Test
  public void testCompactionDoesNotWorkForShortNumbersAndNonHexHashes() {
    String expected = "/resource/123/daka9e610001f70104003acc866d55656d6a5a/get";
    Uri uri = Uri.create("http://localhost:2800" + expected);
    assertEquals(expected, UriCompactionUtil.compactUri(uri, 4, 16, "<>"));
  }
}
