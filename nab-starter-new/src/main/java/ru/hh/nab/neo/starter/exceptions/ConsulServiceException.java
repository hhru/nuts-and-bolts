package ru.hh.nab.neo.starter.exceptions;

public class ConsulServiceException extends RuntimeException {
  public ConsulServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
