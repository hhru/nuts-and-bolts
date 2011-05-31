package ru.hh.nab.testing;

import com.google.inject.Module;
import com.google.inject.Stage;
import java.util.Properties;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientParamBean;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.BasicHttpParams;
import ru.hh.nab.Launcher;

public abstract class JerseyTest {
  protected static Launcher.Instance instance;
  protected static String baseUrl;

  protected JerseyTest() {
    if (instance == null) {
      try {
        instance = createServer();
        System.out.println("=== Test server is bound to port " + instance.port + " ===");
        baseUrl = "http://127.0.0.1:" + instance.port + "/";
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected abstract Properties settings();

  protected abstract Module module();

  protected Properties apiSecurity() {
    return new Properties();
  }

  private Launcher.Instance createServer() throws Exception {
    return Launcher.testMode(Stage.DEVELOPMENT, module(), settings(), apiSecurity());
  }

  protected String baseUrl() {
    return baseUrl;
  }

  protected HttpClient httpClient() {
    BasicHttpParams httpParams = new BasicHttpParams();
    DefaultHttpClient.setDefaultHttpParams(httpParams);

    HttpClientParams.setRedirecting(httpParams, false);
    HttpClientParams.setCookiePolicy(httpParams, CookiePolicy.BROWSER_COMPATIBILITY);

    new ClientParamBean(httpParams).setDefaultHost(new HttpHost("127.0.0.1", instance.port));

    return new DefaultHttpClient(httpParams);
  }
}
