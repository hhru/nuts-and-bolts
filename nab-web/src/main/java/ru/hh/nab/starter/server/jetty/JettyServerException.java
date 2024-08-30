package ru.hh.nab.starter.server.jetty;

final class JettyServerException extends RuntimeException {

  JettyServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
