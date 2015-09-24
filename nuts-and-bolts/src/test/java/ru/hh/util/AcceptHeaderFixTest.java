package ru.hh.util;

import com.google.inject.Module;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.glassfish.grizzly.http.util.Header;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.nab.JerseyResource;
import ru.hh.nab.NabModule;
import ru.hh.nab.testing.JerseyTest;
import java.io.IOException;
import java.util.Properties;

public class AcceptHeaderFixTest extends JerseyTest {

  public static final String BAD_HEADER_1 = "application/xml,application/vnd.wap.xhtml+xml"
          +    ",application/xhtml+xml;profile='http://www.wapforum.org/xhtml',text/html;q=0.9" +
    ",text/plain;q=0.8,image/png,*/*;q=0.5";

  public static final String BAD_HEADER_2 = "text/vnd.wap.wml, text/css, text/ecmascript, image/png, image/gif"
          +    ", image/jpeg; q=0.5, image/x-bmp; q=0.3, image/vnd.wap.wbmp; q=0.2, audio/mid, audio/midi" +
    ", audio/qcelp, audio/vnd.qcp, audio/vnd.qcelp, audio/aac, audio/mp3, audio/x-wav, audio/x-wave" +
    ", audio/mpeg3, audio/mpeg, audio/mpg, audio/amr, application/x-pmd, application/x-cmx, application/x-pmd" +
    ", audio/m4a, audio/mp4, audio/3gpp, audio/3gpp2, audio/mp4a-latm, video/3gpp, video/3gpp2, video/mp4" +
    ", video/mp4v-es, text/vnd.sun.j2me.app-descriptor, text/x-pcs-gcd, multipart/mixed; q=.1" +
    ", multipart/vnd.sprint-pre-cache, application/xhtml+xml; profile=http://www.wapforum.org/xhtml" +
    ", application/vnd.wap.xhtml+xml, application/sdp, application/x-pcs-mcd+xml, text/plain" +
    ", application/vnd.wap.wmlscriptc, application/vnd.wap.wmlc application/vnd.wap.sic, application/vnd.wap.slc" +
    ", application/sia, application/vnd.wap.mms-message, */*";

  @Test
  public void checkWithBadAcceptHeader1() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "test");
    req.setHeader(Header.Accept.toString(), BAD_HEADER_1);
    HttpResponse resp = httpClient().execute(req);
    Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
  }

  @Test
  public void checkWithBadAcceptHeader2() throws IOException {
    HttpGet req = new HttpGet(baseUrl() + "test");
    req.setHeader(Header.Accept.toString(), BAD_HEADER_2);
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

}
