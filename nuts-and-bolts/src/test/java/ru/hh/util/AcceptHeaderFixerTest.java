package ru.hh.util;

import com.google.inject.Module;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.glassfish.grizzly.http.util.Header;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.NabModule;
import ru.hh.nab.TestResource;
import ru.hh.nab.testing.JerseyTest;
import java.io.IOException;
import java.util.Properties;

public class AcceptHeaderFixerTest extends JerseyTest {
  @Test
  public void checkWithBadAcceptHeader() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "test");
    req.setHeader(Header.Accept.toString(), "application/xml,application/vnd.wap.xhtml+xml,application/xhtml+xml;profile='http://www.wapforum.org/xhtml',text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
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
        bind(TestResource.class);
      }
    };
  }

}
