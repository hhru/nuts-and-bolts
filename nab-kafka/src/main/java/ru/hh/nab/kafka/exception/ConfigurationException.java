package ru.hh.nab.kafka.exception;

/**
 * Any invalid configuration that is part of the public API must be a subclass of this class and be part of this
 * package.
 */
public class ConfigurationException extends NabKafkaException {
  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(Throwable cause) {
    super(cause);
  }

  public ConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public ConfigurationException() {
  }
}
