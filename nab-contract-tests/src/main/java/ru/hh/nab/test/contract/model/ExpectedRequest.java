package ru.hh.nab.test.contract.model;

import java.util.List;
import java.util.Map;

public class ExpectedRequest {

  private String method;

  private String path;

  private String body;

  private Map<String, List<String>> headers;

  private Map<String, List<String>> queryParams;

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, List<String>> headers) {
    this.headers = headers;
  }

  public Map<String, List<String>> getQueryParams() {
    return queryParams;
  }

  public void setQueryParams(Map<String, List<String>> queryParams) {
    this.queryParams = queryParams;
  }
}
