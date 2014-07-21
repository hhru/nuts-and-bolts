package ru.hh.util;

import org.junit.Assert;
import org.junit.Test;
import java.net.URISyntaxException;

public class UriToolTest {
  @Test
  public void testCanParseWithFix() throws URISyntaxException {
    String correctUrl = "http://localhost:9100/validate/;http;yaroslavl.hh.ru;80;/vacancy/penelope?utm_campaign=%7B%7Bcampaign_id%7D%7D&utm_medium=cpc&utm_source" +
      "=clickmehhru&utm_content=%7B%7Bbanner_id%7D%7D";
    String badUrl = "http://localhost:9100/validate/;http;yaroslavl.hh.ru;80;/vacancy/penelope?utm_campaign={{campaign_id}}&utm_medium=cpc&utm_source=clickmehhru&utm_content={{banner_id}}";

    Assert.assertEquals(correctUrl, UriTool.getUri(badUrl).toASCIIString());
    Assert.assertEquals(correctUrl, UriTool.getUri(correctUrl).toASCIIString());
  }
}
