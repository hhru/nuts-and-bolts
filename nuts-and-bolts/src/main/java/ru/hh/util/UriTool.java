package ru.hh.util;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class UriTool {

  private UriTool() {}

  // handles badly encoded parameters or fragment parts properly
  // (other methods often broke space characters in values, esp.
  // space values in backurl parameters)
  public static URI getUri(String url) throws URISyntaxException {
    try {
      return URI.create(url);
    } catch (IllegalArgumentException e) {
      // url parameters need to be escaped twice when passed as backurls,
      // otherwise we will definitely get here from jersey calls.
      int qPos = url.indexOf('?'); // queryPos (part after ?)
      int fPos = url.indexOf('#'); // fragmentPos (part after #)
      if (qPos == -1 && fPos == -1) {
        throw e; // no reason to try anything if no query or fragment
      }
      if (fPos > -1 && fPos < qPos) {
        qPos = -1; // question mark is part of fragment, ignore it
      }
      String baseUrl = "";
      if (qPos > 0) {
        baseUrl = url.substring(0, qPos);
      } else if (qPos == -1 && fPos > 0) {
        baseUrl = url.substring(0, fPos);
      }

      String query = null;
      if (qPos > -1) {
        query = url.substring(qPos + 1, fPos > -1 ? fPos : url.length());
      }

      String fragment = null;
      if (fPos > -1) {
        fragment = url.substring(fPos + 1);
      }

      // Note: '?' and '#' are automatically added if needed
      String fixedQueryAndFragment = new URI(null, null, null, query, fragment).toASCIIString();

      return URI.create(baseUrl + fixedQueryAndFragment);
    }
  }
}
