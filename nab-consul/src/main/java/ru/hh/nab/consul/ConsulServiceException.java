package ru.hh.nab.consul;

public class ConsulServiceException extends RuntimeException {
  public ConsulServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
