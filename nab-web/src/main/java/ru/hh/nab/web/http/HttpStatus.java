package ru.hh.nab.web.http;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public enum HttpStatus implements Response.StatusType {
  SERVICE_PARTIALLY_UNAVAILABLE(597, "Service partially unavailable");

  private final int statusCode;
  private final String reasonPhrase;
  private final Status.Family family;

  HttpStatus(int statusCode, String reasonPhrase) {
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
    this.family = Status.Family.familyOf(statusCode);
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public Status.Family getFamily() {
    return family;
  }

  @Override
  public String getReasonPhrase() {
    return reasonPhrase;
  }

  @Override
  public String toString() {
    return reasonPhrase;
  }
}
