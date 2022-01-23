package ru.hh.nab.test.contract.model;

import java.util.List;
import java.util.Map;

public class ExpectedResponse {

  private Integer status;
  private Map<String, List<String>> headers;
  private Object responseBody;

  public ExpectedResponse(Integer status, Object responseBody, Map<String, List<String>> headers) {
    this.status = status;
    this.responseBody = responseBody;
    this.headers = headers;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, List<String>> headers) {
    this.headers = headers;
  }

  public Object getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(Object responseBody) {
    this.responseBody = responseBody;
  }
}
