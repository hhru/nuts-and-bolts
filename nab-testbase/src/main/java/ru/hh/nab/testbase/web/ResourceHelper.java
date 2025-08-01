package ru.hh.nab.testbase.web;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.OK;
import jakarta.ws.rs.core.UriBuilder;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceHelper {

  private static final String HTTP = "http";

  private final Supplier<Integer> serverPort;
  private final Client client;

  public ResourceHelper(int serverPort) {
    this(() -> serverPort);
  }

  public ResourceHelper(Supplier<Integer> serverPort) {
    this.serverPort = serverPort;
    this.client = getClientBuilder().build();
  }

  public ClientBuilder getClientBuilder() {
    return ClientBuilder.newBuilder();
  }

  public String baseUrl() {
    return getServerAddress(serverPort.get()).toString();
  }

  public String baseUrl(String protocol) {
    return getServerAddress(protocol, serverPort.get()).toString();
  }

  public int port() {
    return serverPort.get();
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
    return getServerAddress(HTTP, port);
  }

  public static URI getServerAddress(String protocol, int port) {
    try {
      String hostAddress = InetAddress.getLoopbackAddress().getHostAddress();
      return new URI(protocol, null, hostAddress, port, null, null, null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
