package ru.hh.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class UriTool {

  private UriTool() {}

  // handles badly encoded paths, parameters or fragment parts
  public static URI getUri(String urlString) throws URISyntaxException {
    try {
      return URI.create(urlString);
    } catch (IllegalArgumentException e) {
      // Url was provided without necessary escaping;
      // pass through URL/URI constructors to apply proper escaping
      try {
        URL url = new URL(urlString);
        return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
      } catch (MalformedURLException e1) {
        throw new URISyntaxException(urlString, e1.getMessage());
      }
    }
  }
}
