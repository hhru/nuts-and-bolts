package ru.hh.nab.logging.override;

/**
 * Special exception for {@link LogLevelOverrideExtension} which force to skip overriding without any error.
 * It may be useful in case of handling errors on extension side without excess propagation.
 */
public class SkipLogLevelOverrideException extends RuntimeException {

  public SkipLogLevelOverrideException() {
    super();
  }

  public SkipLogLevelOverrideException(String s) {
    super(s);
  }
}
