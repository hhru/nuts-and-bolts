package ru.hh.nab.security;

import com.google.inject.Module;
import java.io.IOException;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.JerseyResource;
import ru.hh.nab.NabModule;
import ru.hh.nab.testing.JerseyTest;

public class SecureResourceWithImplicitAnonymousTest extends JerseyTest {
  @Test
  public void unauthorizedAccessWithoutKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(403, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void unauthorizedAccessWithKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    req.addHeader("X-Hh-Api-Key", "api-key-none");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(403, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void unauthorizedAccessWithNonexistentKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    req.addHeader("X-Hh-Api-Key", "api-key-nonexistent");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(403, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void unauthorizedAccessWithNonexistentKey2() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "anonymous");
    req.addHeader("X-Hh-Api-Key", "api-key-nonexistent");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(403, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void authorizedAccess() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    req.addHeader("X-Hh-Api-Key", "api-key-authenticate");
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
        bind(JerseyResource.class);
      }
    };
  }

  protected Properties apiSecurity() {
    Properties props = new Properties();
    props.setProperty("api-key-none", "");
    props.setProperty("api-key-authenticate", "authenticate, another");
    return props;
  }
}
