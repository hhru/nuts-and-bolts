package ru.hh.nab.kafka.exception;

/**
 * The base class of all other nab kafka exceptions
 */
public class NabKafkaException extends RuntimeException {
  public NabKafkaException(String message) {
    super(message);
  }

  public NabKafkaException(String message, Throwable cause) {
    super(message, cause);
  }

  public NabKafkaException(Throwable cause) {
    super(cause);
  }

  public NabKafkaException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public NabKafkaException() {
  }
}
