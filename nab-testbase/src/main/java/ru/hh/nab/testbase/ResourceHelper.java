package ru.hh.nab.testbase;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.OK;
import javax.ws.rs.core.UriBuilder;
import static org.eclipse.jetty.util.URIUtil.HTTP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import ru.hh.nab.starter.server.jetty.JettyServer;

public class ResourceHelper {
  private final JettyServer jettyServer;
  private Client client;

  public ResourceHelper(JettyServer jettyServer) {
    this.jettyServer = jettyServer;
    client = getClientBuilder().build();
  }

  public ClientBuilder getClientBuilder() {
    return ClientBuilder.newBuilder();
  }

  public String baseUrl() {
    return getServerAddress(jettyServer.getPort()).toString();
  }

  public int port() {
    return jettyServer.getPort();
  }

  public void assertGet(String url, String expectedResponse) {
    Response response = createRequest(url).get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(expectedResponse, response.readEntity(String.class));
  }

  public void assertGet(Invocation.Builder request, String expectedResponse) {
    Response response = request.get();

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(expectedResponse, response.readEntity(String.class));
  }

  public Response executeGet(String path) {
    return createRequest(path).get();
  }

  public String jerseyUrl(String path, Object... values) {
    return UriBuilder.fromPath(path).build(values).toString();
  }

  public Invocation.Builder createRequest(String url) {
    return target(url).request();
  }

  public WebTarget target(String url) {
    return client.target(baseUrl() + url);
  }

  public Invocation.Builder createRequestFromAbsoluteUrl(String absoluteUrl) {
    return client.target(absoluteUrl).request();
  }

  public static URI getServerAddress(int port) {
    try {
      String hostAddress = InetAddress.getLoopbackAddress().getHostAddress();
      return new URI(HTTP, null, hostAddress, port, null, null, null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
