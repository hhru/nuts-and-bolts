package ru.hh.nab.security;

import com.google.inject.Module;
import com.google.inject.name.Names;
import java.io.IOException;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.JerseyResource;
import ru.hh.nab.NabModule;
import ru.hh.nab.testing.JerseyTest;

public class SecureResourceWithExplicitAnonymousTest extends JerseyTest {
  @Test
  public void unauthorizedAccessWithoutKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(403, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void authorizedAccessWithoutKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "anonymous");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void unauthorizedAccessWithKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "anonymous");
    req.addHeader("X-Hh-Api-Key", "api-key-none");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(403, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void unauthorizedAccessWithNonexistentKey() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "anonymous");
    req.addHeader("X-Hh-Api-Key", "api-key-nonexistent");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void authorizedAccess() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    req.addHeader("X-Hh-Api-Key", "api-key-authenticate");
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void wildcardPermissionAccess() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "authenticate");
    req.addHeader("X-Hh-Api-Key", "api-key-wildcard");
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
        bind(String.class).annotatedWith(Names.named("serviceName")).toInstance("serviceName");
      }
    };
  }

  @Override
  protected Properties apiSecurity() {
    Properties props = new Properties();
    props.setProperty("anonymous", "anonymous");
    props.setProperty("api-key-none", "");
    props.setProperty("api-key-authenticate", "authenticate, another");
    props.setProperty("api-key-wildcard", "*");
    return props;
  }
}
