package ru.hh.nab.xml;

import com.google.inject.Module;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.NabModule;
import ru.hh.nab.testing.JerseyTest;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import org.apache.http.Consts;
import org.apache.http.entity.ContentType;

public class XmlSanitizingTest extends JerseyTest {
  private static final ContentType APPLICATION_XML_UTF8_CONTENT_TYPE = ContentType.create(MediaType.APPLICATION_XML, Consts.UTF_8);

  @Test
  public void testRoot() throws IOException {
    HttpGet get = new HttpGet(baseUrl() + "getRoot");
    String body = execute(get);
    HttpPut put = new HttpPut(baseUrl() + "putRoot");
    put.setEntity(new StringEntity(body, APPLICATION_XML_UTF8_CONTENT_TYPE));
    execute(put);
    Assert.assertEquals(SanitizingResource.SANITIZED_XML_CONTENT, getLastTag().name);
  }

  @Test
  public void testList() throws IOException {
    HttpGet get = new HttpGet(baseUrl() + "getList");
    String body = execute(get);
    HttpPut put = new HttpPut(baseUrl() + "putList");
    put.setEntity(new StringEntity(body, APPLICATION_XML_UTF8_CONTENT_TYPE));
    execute(put);
    Assert.assertEquals(SanitizingResource.SANITIZED_XML_CONTENT, getLastTag().name);
  }

  @Test
  public void testJAXB() throws IOException {
    HttpGet get = new HttpGet(baseUrl() + "getJAXB");
    String body = execute(get);
    HttpPut put = new HttpPut(baseUrl() + "putJAXB");
    put.setEntity(new StringEntity(body, APPLICATION_XML_UTF8_CONTENT_TYPE));
    execute(put);
    Assert.assertEquals(SanitizingResource.SANITIZED_XML_CONTENT, getLastTag().name);
  }

  private String execute(HttpRequestBase request) {
    try {
      HttpResponse resp = httpClient().execute(request);
      Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
      HttpEntity entity = resp.getEntity();
      InputStream stream = null;
      try {
        stream = entity.getContent();
        StringBuilder content = new StringBuilder();
        for (Object line : IOUtils.readLines(stream, Charset.defaultCharset())) {
          content.append((String) line);
        }
        return content.toString();
      } finally {
        IOUtils.closeQuietly(stream);
      }
    } catch (IOException e) {
      Assert.fail();
    }
    return "";
  }

  @Override
  protected Properties settings() {
    Properties props = new Properties();
    props.setProperty("concurrencyLevel", "1");
    return props;
  }

  @Override
  protected Module module() {
    return new NabModule() {
      @Override
      protected void configureApp() {
        bind(SanitizingResource.class);
      }
    };
  }

  private SanitizingResource.Tag getLastTag() {
    SanitizingResource resource = injector().getInstance(SanitizingResource.class);
    Assert.assertFalse(resource.putted.isEmpty());
    return resource.putted.get(resource.putted.size() - 1);
  }
}
