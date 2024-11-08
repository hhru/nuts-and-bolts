package ru.hh.nab.web.exceptions;

public class ConsulServiceException extends RuntimeException {
  public ConsulServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
